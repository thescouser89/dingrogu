package org.jboss.pnc.dingrogu.restadapter.adapter;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCompleteRequest;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCompleteResponse;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateResponse;
import org.jboss.pnc.dingrogu.api.client.EnvironmentDriver;
import org.jboss.pnc.dingrogu.api.client.EnvironmentDriverProducer;
import org.jboss.pnc.dingrogu.api.dto.adapter.EnvironmentDriverCompleteDTO;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.dto.ServerResponseDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

@ApplicationScoped
public class EnvironmentDriverCompleteAdapter implements Adapter<EnvironmentDriverCompleteDTO> {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    EnvironmentDriverProducer environmentDriverProducer;

    @Inject
    EnvironmentDriverCreateAdapter environmentDriverCreateAdapter;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    TaskEndpoint taskEndpoint;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Override
    public String getAdapterName() {
        return "environment-driver-complete";
    }

    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {
        EnvironmentDriverCompleteDTO dto = objectMapper
                .convertValue(startRequest.getPayload(), EnvironmentDriverCompleteDTO.class);

        // get unique id created by environment-driver-create sent back to rex in the start method
        TaskDTO envDriverCreateTask = taskEndpoint
                .getSpecific(environmentDriverCreateAdapter.getRexTaskName(correlationId));
        List<ServerResponseDTO> serverResponses = envDriverCreateTask.getServerResponses();

        if (serverResponses.isEmpty()) {
            throw new RuntimeException(
                    "We didn't get any server response from " + environmentDriverCreateAdapter.getAdapterName() + ": "
                            + correlationId);
        }

        ServerResponseDTO last = serverResponses.get(serverResponses.size() - 1);
        EnvironmentCreateResponse response = objectMapper.convertValue(last.getBody(), EnvironmentCreateResponse.class);

        EnvironmentDriver environmentDriver = environmentDriverProducer
                .getEnvironmentDriver(dto.getEnvironmentDriverUrl());

        EnvironmentCompleteRequest environmentCompleteRequest = EnvironmentCompleteRequest.builder()
                .environmentId(response.getEnvironmentId())
                .enableDebug(dto.isDebugEnabled())
                .build();
        Log.infof("Environment complete request: %s", environmentCompleteRequest);

        EnvironmentCompleteResponse environmentCompleteResponse = environmentDriver.complete(environmentCompleteRequest)
                .toCompletableFuture()
                .join();
        Log.infof("Initial environment complete response: %s", environmentCompleteResponse);

        sendDelayedSuccessfulCallbackToRex(correlationId);
        return Optional.ofNullable(environmentCompleteResponse);
    }

    private void sendDelayedSuccessfulCallbackToRex(String correlationId) {
        managedExecutor.submit(() -> {
            try {
                // sleep for 5 seconds to make sure that Rex has processed the successful start
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                Log.error(e);
            }
            try {
                callbackEndpoint.succeed(getRexTaskName(correlationId), null, null);
            } catch (Exception e) {
                Log.error("Error happened in rex client callback to Rex server for environment driver complete", e);
            }
        });
    }

    @Override
    public void callback(String correlationId, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.BUILD_REX_NOTIFY;
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        // do nothing
    }
}

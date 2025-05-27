package org.jboss.pnc.dingrogu.restadapter.adapter;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.common.log.ProcessStageUtils;
import org.jboss.pnc.dingrogu.api.dto.adapter.ProcessStage;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverSealDTO;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.restadapter.client.RepositoryDriverClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

@ApplicationScoped
public class RepositoryDriverSealAdapter implements Adapter<RepositoryDriverSealDTO> {

    @Inject
    RepositoryDriverClient repositoryDriverClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    ManagedExecutor managedExecutor;

    @Override
    public String getAdapterName() {
        return "repository-driver-seal";
    }

    /**
     * The request to repository driver doesn't support callbacks. We'll have to simulate it!
     *
     * @param correlationId
     * @param startRequest
     */
    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {

        ProcessStageUtils
                .logProcessStageBegin(ProcessStage.SEALING_REPOSITORY_MANAGER_RESULTS.name(), "Sealing repository");
        RepositoryDriverSealDTO repositorySealDTO = objectMapper
                .convertValue(startRequest.getPayload(), RepositoryDriverSealDTO.class);

        repositoryDriverClient.seal(repositorySealDTO.getRepositoryDriverUrl(), repositorySealDTO.getBuildContentId());
        ProcessStageUtils
                .logProcessStageEnd(
                        ProcessStage.SEALING_REPOSITORY_MANAGER_RESULTS.name(),
                        "Repository manager results sealed.");
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
                Log.error("Error happened in rex client callback to Rex server for repository driver seal", e);
                callbackEndpoint.fail(getRexTaskName(correlationId), e.toString(), null);
            }
        });

        return Optional.empty();
    }

    @Override
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.BUILD_REX_NOTIFY;
    }

    /**
     * We're not supposed to use this since the start adapter endpoint will send the callback directly to Rex
     *
     * @param correlationId
     * @param object callback object
     */
    @Override
    public void callback(String correlationId, Object object) {
        throw new UnsupportedOperationException();
    }

    /**
     * Cancellation not supported by repository driver for the seal operation
     * 
     * @param correlationId
     * @param stopRequest
     */
    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
    }
}

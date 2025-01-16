package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverSealDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.RepositoryDriverClient;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.List;

@ApplicationScoped
public class RepositoryDriverSealAdapter implements Adapter<RepositoryDriverSealDTO> {

    @Inject
    RepositoryDriverClient repositoryDriverClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RexClient rexClient;

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
    public void start(String correlationId, StartRequest startRequest) {

        RepositoryDriverSealDTO repositorySealDTO = objectMapper
                .convertValue(startRequest.getPayload(), RepositoryDriverSealDTO.class);

        repositoryDriverClient.seal(repositorySealDTO.getRepositoryDriverUrl(), repositorySealDTO.getBuildContentId());

        managedExecutor.submit(() -> {
            try {
                // sleep for 5 seconds to make sure that Rex has processed the successful start
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                Log.error(e);
            }
            try {
                rexClient.invokeSuccessCallback(getRexTaskName(correlationId), null);
            } catch (Exception e) {
                Log.error("Error happened in rex client callback to Rex server for repository driver seal", e);
            }
        });
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

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateTaskDTO generateRexTask(
            String adapterUrl,
            String correlationId,
            RepositoryDriverSealDTO repositorySealDTO) throws Exception {

        Request startSetup = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repositorySealDTO);

        Request cancelSetup = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repositorySealDTO);

        Request callerNotification = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getNotificationEndpoint(adapterUrl)),
                List.of(TaskHelper.getJsonHeader()),
                null);

        return CreateTaskDTO.builder()
                .name(getRexTaskName(correlationId))
                .remoteStart(startSetup)
                .remoteCancel(cancelSetup)
                .configuration(new ConfigurationDTO())
                .callerNotifications(callerNotification)
                .build();
    }
}

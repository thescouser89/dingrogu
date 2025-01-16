package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryPromoteRequest;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverPromoteDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.RepositoryDriverClient;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@ApplicationScoped
public class RepositoryDriverPromoteAdapter implements Adapter<RepositoryDriverPromoteDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    RepositoryDriverClient repositoryDriverClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RexClient rexClient;

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

        Request callback;
        try {
            callback = new Request(
                    Request.Method.POST,
                    new URI(AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getAdapterName(), correlationId)),
                    List.of(TaskHelper.getJsonHeader()),
                    null);
        } catch (URISyntaxException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }

        RepositoryDriverPromoteDTO repoPromoteDTO = objectMapper
                .convertValue(startRequest.getPayload(), RepositoryDriverPromoteDTO.class);

        // TODO: heartbeat
        RepositoryPromoteRequest promoteRequest = RepositoryPromoteRequest.builder()
                .buildContentId(repoPromoteDTO.getBuildContentId())
                .buildType(repoPromoteDTO.getBuildType())
                .buildCategory(repoPromoteDTO.getBuildCategory())
                .tempBuild(repoPromoteDTO.isTempBuild())
                .callback(callback)
                .heartBeat(null)
                .buildConfigurationId(repoPromoteDTO.getBuildContentId())
                .build();

        repositoryDriverClient.promote(repoPromoteDTO.getRepositoryDriverUrl(), promoteRequest);
    }

    /**
     * We're not supposed to use this since the start adapter endpoint will send the callback directly to Rex
     *
     * @param correlationId
     * @param object callback object
     */
    @Override
    public void callback(String correlationId, Object object) {
        try {
            // RepositoryPromoteResult
            rexClient.invokeSuccessCallback(getRexTaskName(correlationId), object);
        } catch (Exception e) {
            Log.error("Error happened in callback adapter", e);
        }
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateTaskDTO generateRexTask(
            String adapterUrl,
            String correlationId,
            RepositoryDriverPromoteDTO repositoryPromoteDTO) throws Exception {

        Request startSetup = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repositoryPromoteDTO);

        Request cancelSetup = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repositoryPromoteDTO);

        return CreateTaskDTO.builder()
                .name(getRexTaskName(correlationId))
                .remoteStart(startSetup)
                .remoteCancel(cancelSetup)
                .configuration(new ConfigurationDTO())
                .build();
    }
}

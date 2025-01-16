package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.repour.dto.RepourCloneCallback;
import org.jboss.pnc.api.repour.dto.RepourCloneRepositoryRequest;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCloneRepositoryDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCreateRepoResponse;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.RepourClient;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RepourCloneRepositoryAdapter implements Adapter<RepourCloneRepositoryDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    RexClient rexClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RepourCreateRepositoryAdapter repourCreate;

    @Inject
    RepourClient repourClient;

    @Override
    public void start(String correlationId, StartRequest startRequest) {

        Map<String, Object> pastResults = startRequest.getTaskResults();
        Object pastResult = pastResults.get(repourCreate.getRexTaskName(correlationId));
        ServerResponse serverResponse = objectMapper.convertValue(pastResult, ServerResponse.class);
        RepourCreateRepoResponse repourResponse = objectMapper
                .convertValue(serverResponse.getBody(), RepourCreateRepoResponse.class);

        RepourCloneRepositoryDTO dto = objectMapper
                .convertValue(startRequest.getPayload(), RepourCloneRepositoryDTO.class);

        RepourCloneCallback callback = RepourCloneCallback.builder()
                .url(AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getAdapterName(), correlationId))
                .method("POST")
                .build();

        RepourCloneRepositoryRequest request = RepourCloneRepositoryRequest.builder()
                .type("git")
                .originRepoUrl(dto.getExternalUrl())
                .targetRepoUrl(repourResponse.getReadwriteUrl())
                .ref(dto.getRef())
                .callback(callback)
                .build();

        repourClient.cloneRequest(dto.getRepourUrl(), request);
    }

    @Override
    public void callback(String correlationId, Object object) {

        try {
            rexClient.invokeSuccessCallback(getRexTaskName(correlationId), object);
        } catch (Exception e) {
            Log.error("Error happened in callback adapter", e);
        }
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAdapterName() {
        return "repour-clone-repository";
    }

    @Override
    public CreateTaskDTO generateRexTask(String adapterUrl, String correlationId, RepourCloneRepositoryDTO repourDTO)
            throws Exception {

        Request startCloneScm = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repourDTO);

        Request cancelCloneScm = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()));

        Request callerNotification = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getNotificationEndpoint(adapterUrl)),
                List.of(TaskHelper.getJsonHeader()),
                null);

        return CreateTaskDTO.builder()
                .name(getRexTaskName(correlationId))
                .remoteStart(startCloneScm)
                .remoteCancel(cancelCloneScm)
                .configuration(ConfigurationDTO.builder().passResultsOfDependencies(true).build())
                .callerNotifications(callerNotification)
                .build();
    }
}

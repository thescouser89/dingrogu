package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.repour.dto.RepourCloneCallback;
import org.jboss.pnc.api.repour.dto.RepourCloneRepositoryRequest;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCloneRepositoryDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCreateRepoResponse;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.restadapter.client.RepourClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class RepourCloneRepositoryAdapter implements Adapter<RepourCloneRepositoryDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RepourCreateRepositoryAdapter repourCreate;

    @Inject
    RepourClient repourClient;

    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {

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

        return Optional.empty();
    }

    @Override
    public void callback(String correlationId, Object object) {

        try {
            callbackEndpoint.succeed(getRexTaskName(correlationId), object, null);
        } catch (Exception e) {
            Log.error("Error happened in callback adapter", e);
        }
    }

    @Override
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.REPOSITORY_CREATION_REX_NOTIFY;
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

    /**
     * We read past results to build final request
     * 
     * @return true
     */
    @Override
    public boolean shouldGetResultsFromDependencies() {
        return true;
    }
}

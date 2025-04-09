package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.reqour.dto.InternalSCMCreationResponse;
import org.jboss.pnc.api.reqour.dto.RepositoryCloneRequest;
import org.jboss.pnc.dingrogu.api.dto.adapter.ReqourCloneRepositoryDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.ReqourClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ReqourCloneRepositoryAdapter implements Adapter<ReqourCloneRepositoryDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ReqourCreateRepositoryAdapter reqourCreate;

    @Inject
    ReqourClient reqourClient;

    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {

        Map<String, Object> pastResults = startRequest.getTaskResults();
        Object pastResult = pastResults.get(reqourCreate.getRexTaskName(correlationId));
        InternalSCMCreationResponse reqourResponse = objectMapper
                .convertValue(pastResult, InternalSCMCreationResponse.class);

        ReqourCloneRepositoryDTO dto = objectMapper
                .convertValue(startRequest.getPayload(), ReqourCloneRepositoryDTO.class);

        Request callback;
        try {
            URI callbackUri = new URI(
                    AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getAdapterName(), correlationId));
            callback = Request.builder()
                    .method(Request.Method.POST)
                    .uri(callbackUri)
                    .headers(TaskHelper.getHTTPHeaders())
                    .build();
        } catch (URISyntaxException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }

        RepositoryCloneRequest request = RepositoryCloneRequest.builder()
                .originRepoUrl(dto.getExternalUrl())
                .targetRepoUrl(reqourResponse.getReadwriteUrl())
                .ref(dto.getRef())
                .taskId(getRexTaskName(correlationId))
                .callback(callback)
                .build();

        Log.infof("Calling Reqour clone with %s", request);

        reqourClient.cloneRequest(dto.getReqourUrl(), request);

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

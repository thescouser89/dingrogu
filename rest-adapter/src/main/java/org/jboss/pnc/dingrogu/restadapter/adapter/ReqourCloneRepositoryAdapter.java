package org.jboss.pnc.dingrogu.restadapter.adapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.reqour.dto.InternalSCMCreationResponse;
import org.jboss.pnc.api.reqour.dto.RepositoryCloneRequest;
import org.jboss.pnc.api.reqour.dto.RepositoryCloneResponse;
import org.jboss.pnc.dingrogu.api.dto.adapter.ReqourCloneRepositoryDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.ReqourClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

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

        reqourClient.cloneRequest(dto.getReqourUrl(), request);

        return Optional.empty();
    }

    @Override
    public void callback(String correlationId, Object object) {
        try {
            RepositoryCloneResponse response = objectMapper.convertValue(object, RepositoryCloneResponse.class);
            try {
                if (response != null && response.getCallback().getStatus().isSuccess()) {
                    callbackEndpoint.succeed(getRexTaskName(correlationId), object, null, null);
                } else {
                    callbackEndpoint.fail(getRexTaskName(correlationId), object, null, null);
                }
            } catch (Exception e) {
                Log.error("Error happened in callback adapter", e);
            }
        } catch (IllegalArgumentException e) {
            // if we cannot cast object to RepositoryCloneResponse, it's probably a failure
            try {
                callbackEndpoint.fail(getRexTaskName(correlationId), object, null, null);
            } catch (Exception ex) {
                Log.error("Error happened in callback adapter", ex);
            }
        }
    }

    @Override
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.REPOSITORY_CREATION_REX_NOTIFY;
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {

        ReqourCloneRepositoryDTO dto = objectMapper
                .convertValue(stopRequest.getPayload(), ReqourCloneRepositoryDTO.class);
        reqourClient.cancel(dto.getReqourUrl(), getRexTaskName(correlationId));
    }

    @Override
    public String getAdapterName() {
        return "reqour-clone-repository";
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

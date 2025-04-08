package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.reqour.dto.InternalSCMCreationRequest;
import org.jboss.pnc.api.reqour.dto.InternalSCMCreationResponse;
import org.jboss.pnc.dingrogu.api.dto.adapter.ReqourCreateRepositoryDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.common.GitUrlParser;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.ReqourClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@ApplicationScoped
public class ReqourCreateRepositoryAdapter implements Adapter<ReqourCreateRepositoryDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    ReqourClient reqourClient;

    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {
        ReqourCreateRepositoryDTO repourCreateDTO = objectMapper
                .convertValue(startRequest.getPayload(), ReqourCreateRepositoryDTO.class);

        Request callback;
        try {
            callback = new Request(
                    Request.Method.POST,
                    new URI(AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getAdapterName(), correlationId)),
                    TaskHelper.getHTTPHeaders(),
                    null);
        } catch (URISyntaxException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }

        InternalSCMCreationRequest request = InternalSCMCreationRequest.builder()
                .project(getProjectName(repourCreateDTO.getExternalUrl()))
                .callback(callback)
                .taskId(getRexTaskName(correlationId))
                .build();

        reqourClient.createRepository(repourCreateDTO.getRepourUrl(), request);

        return Optional.empty();
    }

    @Override
    public void callback(String correlationId, Object object) {
        try {
            InternalSCMCreationResponse response = objectMapper.convertValue(object, InternalSCMCreationResponse.class);
            try {
                if (response == null || !response.getCallback().getStatus().isSuccess()) {
                    callbackEndpoint.fail(getRexTaskName(correlationId), object, null);
                } else {
                    Log.infof("Repo creation response: %s", response.toString());
                    callbackEndpoint.succeed(getRexTaskName(correlationId), object, null);
                }
            } catch (Exception e) {
                Log.error("Error happened in rex client callback to Rex server for repository creation", e);
            }
        } catch (IllegalArgumentException e) {
            // if we cannot cast object to InternalSCMCreationResponse, it's probably a failure
            try {
                callbackEndpoint.fail(getRexTaskName(correlationId), object, null);
            } catch (Exception ex) {
                Log.error("Error happened in callback adapter", ex);
            }
        }
    }

    /**
     * There's nothing to cancel, this process is synchronous
     *
     * @param correlationId
     * @param stopRequest
     */
    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        return;
    }

    @Override
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.REPOSITORY_CREATION_REX_NOTIFY;
    }

    @Override
    public String getAdapterName() {
        return "repour-create-repository";
    }

    private static String getProjectName(String externalUrl) {
        return GitUrlParser.generateInternalGitRepoName(externalUrl);
    }
}

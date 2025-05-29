package org.jboss.pnc.dingrogu.restadapter.adapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.causeway.dto.push.BuildPushCompleted;
import org.jboss.pnc.api.causeway.dto.push.PushResult;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.dto.adapter.OrchBuildPushResultDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.OrchClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

@ApplicationScoped
public class OrchBuildPushResultAdapter implements Adapter<OrchBuildPushResultDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CausewayBuildPushAdapter causewayBuildPushAdapter;

    @Inject
    OrchClient orchClient;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Override
    public String getAdapterName() {
        return "orch-push-result";
    }

    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {

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
        OrchBuildPushResultDTO dto = objectMapper.convertValue(startRequest.getPayload(), OrchBuildPushResultDTO.class);

        Map<String, Object> pastResults = startRequest.getTaskResults();
        Object pastResult = pastResults.get(causewayBuildPushAdapter.getRexTaskName(correlationId));
        PushResult pushResult = objectMapper.convertValue(pastResult, PushResult.class);

        BuildPushCompleted pushCompleted = BuildPushCompleted.builder()
                .operationId(dto.getOperationId())
                .brewBuildUrl(pushResult.getBrewBuildUrl())
                .brewBuildId(pushResult.getBrewBuildId())
                .callback(callback)
                .build();

        orchClient.submitBuildPushResult(dto.getOrchUrl(), dto.getBuildId(), pushCompleted);
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

    /**
     * Do nothing, we can't really cancel this?
     * 
     * @param correlationId
     * @param stopRequest
     */
    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
    }

    @Override
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.BREW_PUSH_REX_NOTIFY;
    }

    @Override
    public boolean shouldGetResultsFromDependencies() {
        return true;
    }
}

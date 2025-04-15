package org.jboss.pnc.dingrogu.restadapter.adapter;

import java.net.URI;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.causeway.dto.push.BuildPushRequest;
import org.jboss.pnc.api.causeway.dto.push.PushResult;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.dingrogu.api.client.AuthorizationClientHttpFactory;
import org.jboss.pnc.dingrogu.api.client.CausewayProducer;
import org.jboss.pnc.dingrogu.api.dto.adapter.BrewPushDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;

@ApplicationScoped
public class CausewayBuildPushAdapter implements Adapter<BrewPushDTO> {

    private final ObjectMapper objectMapper;

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    CausewayProducer causewayProducer;

    @Inject
    AuthorizationClientHttpFactory authorizationClientHttpFactory;

    @jakarta.inject.Inject
    public CausewayBuildPushAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getAdapterName() {
        return "causeway-brew-push";
    }

    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {
        BrewPushDTO brewPushDTO = objectMapper.convertValue(startRequest.getPayload(), BrewPushDTO.class);

        String callbackUrl = AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getAdapterName(), correlationId);
        Request callback = new Request(Request.Method.POST, URI.create(callbackUrl));

        BuildPushRequest request = BuildPushRequest.builder()
                .id(correlationId)
                .buildId(brewPushDTO.getBuildId())
                .tagPrefix(brewPushDTO.getTagPrefix())
                .reimport(brewPushDTO.isReimport())
                .username(brewPushDTO.getUsername())
                .callback(callback)
                .heartbeat(null)
                .build();

        causewayProducer.getCauseway(brewPushDTO.getCausewayUrl()).importBuild(request);
        return Optional.empty();
    }

    @Override
    public void callback(String correlationId, Object o) {
        try {

            PushResult pushResult = objectMapper.convertValue(o, PushResult.class);

            if (pushResult != null && pushResult.getResult() != null
                    && pushResult.getResult() == ResultStatus.SUCCESS) {
                callbackEndpoint.succeed(getRexTaskName(correlationId), pushResult, null);
            } else {
                callbackEndpoint.fail(getRexTaskName(correlationId), pushResult, null);
            }
        } catch (Exception e) {
            Log.error("Error while receiving callback", e);
            callbackEndpoint.fail(getRexTaskName(correlationId), o, null);
        }
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.BREW_PUSH_REX_NOTIFY;
    }
}

package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.constants.BuildConfigurationParameterKeys;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.reqour.dto.AdjustRequest;
import org.jboss.pnc.api.reqour.dto.AdjustResponse;
import org.jboss.pnc.api.reqour.dto.InternalGitRepositoryUrl;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.adapter.ReqourAdjustDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.GitUrlParser;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.ReqourClient;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ReqourAdjustAdapter implements Adapter<ReqourAdjustDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RexClient rexClient;

    @Inject
    ReqourClient reqourClient;

    @Override
    public String getAdapterName() {
        return "reqour-adjust";
    }

    @Override
    public void start(String correlationId, StartRequest startRequest) {
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

        ReqourAdjustDTO reqourAdjustDTO = objectMapper.convertValue(startRequest.getPayload(), ReqourAdjustDTO.class);

        String callbackUrl = AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getAdapterName(), correlationId);

        // Generate DTO to submit to Reqour
        InternalGitRepositoryUrl internalUrl = InternalGitRepositoryUrl.builder()
                .readonlyUrl(GitUrlParser.scmRepoURLReadOnly(reqourAdjustDTO.getScmRepoURL()))
                .readwriteUrl(reqourAdjustDTO.getScmRepoURL())
                .build();

        // Map <String, String> to <BuildConfigurationParameterKeys, String>
        Map<BuildConfigurationParameterKeys, String> buildConfigParameters = new HashMap<>();
        for (BuildConfigurationParameterKeys key : BuildConfigurationParameterKeys.values()) {
            String value = reqourAdjustDTO.getGenericParameters().get(key.name());
            if (value != null) {
                buildConfigParameters.put(key, value);
            }
        }

        AdjustRequest request = AdjustRequest.builder()
                .internalUrl(internalUrl)
                .ref(reqourAdjustDTO.getScmRevision())
                .callback(callback)
                .sync(reqourAdjustDTO.isPreBuildSyncEnabled())
                .originRepoUrl(reqourAdjustDTO.getOriginRepoURL())
                .buildConfigParameters(buildConfigParameters)
                .tempBuild(reqourAdjustDTO.isTempBuild())
                .alignmentPreference(reqourAdjustDTO.getAlignmentPreference())
                .taskId(reqourAdjustDTO.getId())
                .buildType(reqourAdjustDTO.getBuildType())
                .pncDefaultAlignmentParameters(reqourAdjustDTO.getDefaultAlignmentParams())
                .brewPullActive(reqourAdjustDTO.isBrewPullActive())
                .build();

        // Send to Reqour
        reqourClient.adjust(reqourAdjustDTO.getReqourUrl(), request);
    }

    @Override
    public void callback(String correlationId, Object object) {

        try {
            AdjustResponse response = objectMapper.convertValue(object, AdjustResponse.class);
            try {
                rexClient.invokeSuccessCallback(getRexTaskName(correlationId), response);
            } catch (Exception e) {
                Log.error("Error happened in callback adapter", e);
            }
        } catch (IllegalArgumentException e) {
            // if we cannot cast object to AdjustResponse, it's probably a failure
            try {
                rexClient.invokeFailCallback(getRexTaskName(correlationId), object);
            } catch (Exception ex) {
                Log.error("Error happened in callback adapter", ex);
            }
        }
    }

    @Override
    // TODO
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }
}

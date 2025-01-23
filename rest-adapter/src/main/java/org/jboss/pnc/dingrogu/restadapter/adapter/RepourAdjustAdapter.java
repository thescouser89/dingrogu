package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.repour.dto.RepourAdjustCallback;
import org.jboss.pnc.api.repour.dto.RepourAdjustInternalUrl;
import org.jboss.pnc.api.repour.dto.RepourAdjustRequest;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourAdjustDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourAdjustResponse;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.GitUrlParser;
import org.jboss.pnc.dingrogu.restadapter.client.RepourClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

@ApplicationScoped
public class RepourAdjustAdapter implements Adapter<RepourAdjustDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    RepourClient repourClient;

    @Override
    public String getAdapterName() {
        return "repour-adjust";
    }

    @Override
    public void start(String correlationId, StartRequest startRequest) {
        RepourAdjustDTO repourAdjustDTO = objectMapper.convertValue(startRequest.getPayload(), RepourAdjustDTO.class);

        String callbackUrl = AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getAdapterName(), correlationId);

        // Generate DTO to submit to Repour
        RepourAdjustInternalUrl internalUrl = RepourAdjustInternalUrl.builder()
                .readonly(GitUrlParser.scmRepoURLReadOnly(repourAdjustDTO.getScmRepoURL()))
                .readwrite(repourAdjustDTO.getScmRepoURL())
                .build();

        RepourAdjustRequest request = RepourAdjustRequest.builder()
                .internalUrl(internalUrl)
                .ref(repourAdjustDTO.getScmRevision())
                .callback(RepourAdjustCallback.builder().url(callbackUrl).build())
                .sync(repourAdjustDTO.isPreBuildSyncEnabled())
                .originRepoUrl(repourAdjustDTO.getOriginRepoURL())
                .adjustParameters(repourAdjustDTO.getGenericParameters())
                .tempBuild(repourAdjustDTO.isTempBuild())
                .tempBuildTimestamp(null)
                .alignmentPreference(repourAdjustDTO.getAlignmentPreference())
                .taskId(repourAdjustDTO.getId())
                .buildType(repourAdjustDTO.getBuildType())
                .defaultAlignmentParams(repourAdjustDTO.getDefaultAlignmentParams())
                .brewPullActive(repourAdjustDTO.isBrewPullActive())
                .build();

        // Send to Repour
        repourClient.adjust(repourAdjustDTO.getRepourUrl(), request);
    }

    @Override
    public void callback(String correlationId, Object object) {

        try {
            RepourAdjustResponse response = objectMapper.convertValue(object, RepourAdjustResponse.class);
            try {
                callbackEndpoint.succeed(getRexTaskName(correlationId), response, null);
            } catch (Exception e) {
                Log.error("Error happened in callback adapter", e);
            }
        } catch (IllegalArgumentException e) {
            try {
                callbackEndpoint.fail(getRexTaskName(correlationId), object, null);
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

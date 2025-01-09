package org.jboss.pnc.dingrogu.restadapter.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.repour.dto.RepourAdjustCallback;
import org.jboss.pnc.api.repour.dto.RepourAdjustInternalUrl;
import org.jboss.pnc.api.repour.dto.RepourAdjustRequest;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourAdjustDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourAdjustResponse;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.GitUrlParser;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.RepourClient;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.List;

@ApplicationScoped
public class RepourAdjustAdapter implements Adapter<RepourAdjustDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    RepourClient repourClient;

    @Override
    public String getName() {
        return "repour-adjust";
    }

    @Override
    public void start(String correlationId, StartRequest startRequest) {
        RepourAdjustDTO repourAdjustDTO = (RepourAdjustDTO) startRequest.getPayload();

        String callbackUrl = AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getName(), correlationId);

        // Generate DTO to submit to Repour
        RepourAdjustInternalUrl internalUrl = RepourAdjustInternalUrl.builder()
                .readonly(GitUrlParser.scmRepoURLReadOnly(repourAdjustDTO.getScmRepoURL()))
                .readwrite(repourAdjustDTO.getRepourUrl())
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
        RepourAdjustResponse response = (RepourAdjustResponse) object;

        // TODO: send result to Rex via the positive/negative callback
    }

    @Override
    // TODO
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateTaskDTO generateRexTask(String adapterUrl, String correlationId, RepourAdjustDTO repourAdjustDTO)
            throws Exception {
        Request startAdjust = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repourAdjustDTO);

        Request cancelAdjust = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repourAdjustDTO);

        return CreateTaskDTO.builder()
                .name(getName())
                .remoteStart(startAdjust)
                .remoteCancel(cancelAdjust)
                .configuration(new ConfigurationDTO())
                .build();
    }
}

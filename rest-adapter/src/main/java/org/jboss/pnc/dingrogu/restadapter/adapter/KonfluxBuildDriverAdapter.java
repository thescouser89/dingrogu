package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.konfluxbuilddriver.dto.BuildCompleted;
import org.jboss.pnc.api.konfluxbuilddriver.dto.BuildRequest;
import org.jboss.pnc.api.konfluxbuilddriver.dto.BuildResponse;
import org.jboss.pnc.api.konfluxbuilddriver.dto.CancelRequest;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateResponse;
import org.jboss.pnc.api.reqour.dto.AdjustResponse;
import org.jboss.pnc.dingrogu.api.client.KonfluxBuildDriver;
import org.jboss.pnc.dingrogu.api.client.KonfluxBuildDriverProducer;
import org.jboss.pnc.dingrogu.api.dto.adapter.KonfluxBuildDriverDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.dto.ServerResponseDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class KonfluxBuildDriverAdapter implements Adapter<KonfluxBuildDriverDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    KonfluxBuildDriverProducer konfluxBuildDriverProducer;

    @Inject
    ReqourAdjustAdapter reqourAdjustAdapter;

    @Inject
    RepositoryDriverSetupAdapter repositoryDriverSetupAdapter;

    @Inject
    TaskEndpoint taskEndpoint;

    @Override
    public String getAdapterName() {
        return "konflux-build-driver";
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
        KonfluxBuildDriverDTO dto = objectMapper.convertValue(startRequest.getPayload(), KonfluxBuildDriverDTO.class);

        Map<String, Object> pastResults = startRequest.getTaskResults();
        Object repoDriverSetup = pastResults.get(repositoryDriverSetupAdapter.getRexTaskName(correlationId));
        RepositoryCreateResponse repositoryResponse = objectMapper
                .convertValue(repoDriverSetup, RepositoryCreateResponse.class);

        Object alignSetup = pastResults.get(reqourAdjustAdapter.getRexTaskName(correlationId));
        ServerResponse serverResponseAlign = objectMapper.convertValue(alignSetup, ServerResponse.class);
        AdjustResponse alignResponse = objectMapper.convertValue(serverResponseAlign.getBody(), AdjustResponse.class);

        KonfluxBuildDriver konfluxBuildDriver = konfluxBuildDriverProducer
                .getKonfluxBuildDriver(dto.getKonfluxBuildDriverUrl());

        BuildRequest buildRequest = BuildRequest.builder()
                .repositoryBuildContentId(dto.getBuildContentId())
                .scmUrl(alignResponse.getInternalUrl().getReadonlyUrl())
                .scmRevision(alignResponse.getDownstreamCommit())
                .buildScript(dto.getBuildScript())
                .buildTool(dto.getBuildTool())
                .recipeImage(dto.getRecipeImage())
                .podMemoryOverride(dto.getPodMemoryOverride())
                .repositoryDeployUrl(repositoryResponse.getRepositoryDeployUrl())
                .repositoryDependencyUrl(repositoryResponse.getRepositoryDependencyUrl())
                .namespace(dto.getNamespace())
                .buildToolVersion(dto.getBuildToolVersion())
                .javaVersion(dto.getJavaVersion())
                .completionCallback(callback)
                .build();
        Log.infof("Konflux build request: %s", buildRequest);

        BuildResponse response = konfluxBuildDriver.build(buildRequest);
        Log.infof("Konflux initial response: %s", response);
        return Optional.ofNullable(response);
    }

    @Override
    public void callback(String correlationId, Object object) {
        try {
            BuildCompleted response = objectMapper.convertValue(object, BuildCompleted.class);
            Log.infof("Konflux response: %s", response);
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
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.BUILD_REX_NOTIFY;
    }

    @Override
    // TODO
    public void cancel(String correlationId, StopRequest stopRequest) {

        // get own unique id created by konflux-build-driver sent back to rex in the start method
        TaskDTO ownTask = taskEndpoint.getSpecific(getRexTaskName(correlationId));
        List<ServerResponseDTO> serverResponses = ownTask.getServerResponses();

        if (serverResponses.isEmpty()) {
            throw new RuntimeException("We didn't get any server response from konflux-build-driver: " + correlationId);
        }

        ServerResponseDTO last = serverResponses.get(serverResponses.size() - 1);
        BuildResponse konfluxResponse = objectMapper.convertValue(last.getBody(), BuildResponse.class);

        KonfluxBuildDriverDTO dto = objectMapper.convertValue(stopRequest.getPayload(), KonfluxBuildDriverDTO.class);
        KonfluxBuildDriver konfluxBuildDriver = konfluxBuildDriverProducer
                .getKonfluxBuildDriver(dto.getKonfluxBuildDriverUrl());

        CancelRequest cancelRequest = CancelRequest.builder()
                .namespace(konfluxResponse.getNamespace())
                .pipelineId(konfluxResponse.getPipelineId())
                .build();
        konfluxBuildDriver.cancel(cancelRequest);
    }

    @Override
    public boolean shouldGetResultsFromDependencies() {
        return true;
    }
}

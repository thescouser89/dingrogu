package org.jboss.pnc.dingrogu.restadapter.adapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.builddriver.dto.BuildCancelRequest;
import org.jboss.pnc.api.builddriver.dto.BuildCompleted;
import org.jboss.pnc.api.builddriver.dto.BuildRequest;
import org.jboss.pnc.api.builddriver.dto.BuildResponse;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateResult;
import org.jboss.pnc.api.reqour.dto.AdjustResponse;
import org.jboss.pnc.common.log.ProcessStageUtils;
import org.jboss.pnc.dingrogu.api.client.BuildDriver;
import org.jboss.pnc.dingrogu.api.client.BuildDriverProducer;
import org.jboss.pnc.dingrogu.api.dto.adapter.BuildDriverDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.ProcessStage;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.dto.ServerResponseDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

@ApplicationScoped
public class BuildDriverAdapter implements Adapter<BuildDriverDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    BuildDriverProducer buildDriverProducer;

    @Inject
    ReqourAdjustAdapter reqourAdjustAdapter;

    @Inject
    EnvironmentDriverCreateAdapter environmentDriverCreateAdapter;

    @Inject
    TaskEndpoint taskEndpoint;

    @Override
    public String getAdapterName() {
        return "build-driver";
    }

    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {

        ProcessStageUtils.logProcessStageBegin(ProcessStage.BUILD_SETTING_UP.name(), "Starting build");

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
        BuildDriverDTO dto = objectMapper.convertValue(startRequest.getPayload(), BuildDriverDTO.class);

        Map<String, Object> pastResults = startRequest.getTaskResults();
        Object envDriverCreate = pastResults.get(environmentDriverCreateAdapter.getRexTaskName(correlationId));
        EnvironmentCreateResult environmentCreateResponse = objectMapper
                .convertValue(envDriverCreate, EnvironmentCreateResult.class);

        Object reqourAdjust = pastResults.get(reqourAdjustAdapter.getRexTaskName(correlationId));
        AdjustResponse adjustResponse = objectMapper.convertValue(reqourAdjust, AdjustResponse.class);

        BuildDriver buildDriver = buildDriverProducer.getBuildDriver(dto.getBuildDriverUrl());

        BuildRequest buildRequest = BuildRequest.builder()
                .projectName(dto.getProjectName())
                .scmUrl(adjustResponse.getInternalUrl().getReadonlyUrl())
                .scmRevision(adjustResponse.getDownstreamCommit())
                .scmTag(adjustResponse.getTag())
                .command(dto.getBuildCommand())
                .workingDirectory(environmentCreateResponse.getWorkingDirectory())
                .environmentBaseUrl(environmentCreateResponse.getEnvironmentBaseUri().toString())
                .debugEnabled(dto.isDebugEnabled())
                .completionCallback(callback)
                .heartbeatConfig(startRequest.getHeartbeatConfig())
                .build();
        Log.infof("Build request: %s", buildRequest);

        BuildResponse buildResponse = buildDriver.build(buildRequest).toCompletableFuture().join();
        Log.infof("Initial build response: %s", buildResponse);
        return Optional.ofNullable(buildResponse);
    }

    @Override
    public void callback(String correlationId, Object object) {
        ProcessStageUtils.logProcessStageEnd(ProcessStage.BUILD_SETTING_UP.name(), "Build completed.");
        try {
            BuildCompleted response = objectMapper.convertValue(object, BuildCompleted.class);
            Log.infof("Build response: %s", response);
            try {
                if (response == null || !response.getBuildStatus().isSuccess()) {
                    callbackEndpoint.fail(getRexTaskName(correlationId), response, null, null);
                } else {
                    callbackEndpoint.succeed(getRexTaskName(correlationId), response, null, null);
                }
            } catch (Exception e) {
                Log.error("Error happened in callback adapter", e);
            }
        } catch (IllegalArgumentException e) {
            try {
                callbackEndpoint.fail(getRexTaskName(correlationId), object, null, null);
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
    public void cancel(String correlationId, StopRequest stopRequest) {
        // get own unique id created by build-driver sent back to rex in the start method
        TaskDTO ownTask = taskEndpoint.getSpecific(getRexTaskName(correlationId));
        List<ServerResponseDTO> serverResponses = ownTask.getServerResponses();

        if (serverResponses.isEmpty()) {
            throw new RuntimeException(
                    "We didn't get any server response from " + getAdapterName() + ": " + correlationId);
        }

        ServerResponseDTO last = serverResponses.get(serverResponses.size() - 1);
        BuildResponse buildResponse = objectMapper.convertValue(last.getBody(), BuildResponse.class);

        BuildDriverDTO dto = objectMapper.convertValue(stopRequest.getPayload(), BuildDriverDTO.class);
        BuildDriver buildDriver = buildDriverProducer.getBuildDriver(dto.getBuildDriverUrl());

        Map<String, Object> pastResults = stopRequest.getTaskResults();
        Object envDriverCreate = pastResults.get(environmentDriverCreateAdapter.getRexTaskName(correlationId));
        EnvironmentCreateResult environmentCreateResponse = objectMapper
                .convertValue(envDriverCreate, EnvironmentCreateResult.class);

        BuildCancelRequest buildCancelRequest = BuildCancelRequest.builder()
                .buildEnvironmentBaseUrl(environmentCreateResponse.getEnvironmentBaseUri().toString())
                .buildExecutionId(buildResponse.getBuildExecutionId())
                .build();
        buildDriver.cancel(buildCancelRequest);
    }

    @Override
    public boolean shouldGetResultsFromDependencies() {
        return true;
    }

    @Override
    public boolean shouldUseHeartbeat() {
        return true;
    }

    /**
     * Increase the heartbeat tolerance for build driver to give more time for the build agent to send heartbeats
     *
     * @return increased tolerance
     */
    @Override
    public int heartbeatTolerance() {
        return 10;
    }
}

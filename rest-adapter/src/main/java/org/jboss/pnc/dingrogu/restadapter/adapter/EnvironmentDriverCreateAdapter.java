package org.jboss.pnc.dingrogu.restadapter.adapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateRequest;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateResponse;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateResult;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateResponse;
import org.jboss.pnc.common.log.ProcessStageUtils;
import org.jboss.pnc.dingrogu.api.client.EnvironmentDriver;
import org.jboss.pnc.dingrogu.api.client.EnvironmentDriverProducer;
import org.jboss.pnc.dingrogu.api.dto.adapter.EnvironmentDriverCreateDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.ProcessStage;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.dto.ServerResponseDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

@ApplicationScoped
public class EnvironmentDriverCreateAdapter implements Adapter<EnvironmentDriverCreateDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    EnvironmentDriverProducer environmentDriverProducer;

    @Inject
    RepositoryDriverSetupAdapter repositoryDriverSetupAdapter;

    @Inject
    TaskEndpoint taskEndpoint;

    @Override
    public String getAdapterName() {
        return "environment-driver-create";
    }

    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {

        ProcessStageUtils
                .logProcessStageBegin(ProcessStage.BUILD_ENV_SETTING_UP.name(), "Starting the build container");
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
        EnvironmentDriverCreateDTO dto = objectMapper
                .convertValue(startRequest.getPayload(), EnvironmentDriverCreateDTO.class);

        Map<String, Object> pastResults = startRequest.getTaskResults();
        Object repoDriverSetup = pastResults.get(repositoryDriverSetupAdapter.getRexTaskName(correlationId));
        RepositoryCreateResponse repositoryResponse = objectMapper
                .convertValue(repoDriverSetup, RepositoryCreateResponse.class);

        EnvironmentDriver environmentDriver = environmentDriverProducer
                .getEnvironmentDriver(dto.getEnvironmentDriverUrl());

        EnvironmentCreateRequest environmentCreateRequest = EnvironmentCreateRequest.builder()
                .environmentLabel(dto.getEnvironmentLabel().toLowerCase(Locale.ROOT)) // toLowerCase because pod names
                // have to be in lowercase
                .imageId(dto.getEnvironmentImage())
                .repositoryDependencyUrl(repositoryResponse.getRepositoryDependencyUrl())
                .repositoryDeployUrl(repositoryResponse.getRepositoryDeployUrl())
                .repositoryBuildContentId(dto.getBuildContentId())
                .podMemoryOverride(dto.getPodMemoryOverride())
                .allowSshDebug(dto.isDebugEnabled())
                .buildConfigId(dto.getBuildConfigId())
                .sidecarEnabled(repositoryResponse.isSidecarEnabled())
                .sidecarArchiveEnabled(repositoryResponse.isSidecarArchiveEnabled())
                .completionCallback(callback)
                .build();
        Log.infof("Environment create request: %s", environmentCreateRequest);

        EnvironmentCreateResponse environmentCreateResponse = environmentDriver.build(environmentCreateRequest)
                .toCompletableFuture()
                .join();
        Log.infof("Initial environment create response: %s", environmentCreateResponse);
        return Optional.ofNullable(environmentCreateResponse);
    }

    @Override
    public void callback(String correlationId, Object object) {
        ProcessStageUtils.logProcessStageEnd(ProcessStage.BUILD_ENV_SETTING_UP.name(), "Build environment prepared.");
        try {
            EnvironmentCreateResult response = objectMapper.convertValue(object, EnvironmentCreateResult.class);
            Log.infof("Environment create response: %s", response);
            try {
                if (response == null || response.getStatus() == null) {
                    Log.error("Environment response or status is null: " + response);
                    callbackEndpoint.fail(getRexTaskName(correlationId), response, null, null);
                    return;
                }
                switch (response.getStatus()) {
                    case SUCCESS -> callbackEndpoint.succeed(getRexTaskName(correlationId), response, null, null);

                    // with rollback (if configured)
                    // TODO should FAILED status from ENV. Driver skip rollback like in other Adapters?
                    case FAILED, TIMED_OUT, CANCELLED, SYSTEM_ERROR ->
                        callbackEndpoint.fail(getRexTaskName(correlationId), response, null, null);
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
        // get own unique id created by environment-driver-create sent back to rex in the start method
        TaskDTO ownTask = taskEndpoint.getSpecific(getRexTaskName(correlationId));
        List<ServerResponseDTO> serverResponses = ownTask.getServerResponses();

        if (serverResponses.isEmpty()) {
            throw new RuntimeException(
                    "We didn't get any server response from " + getAdapterName() + ": " + correlationId);
        }

        // Only the STARTING server responses contain the initial EnvironmentCreateResponse. it's not in the callback from env-driver to dingrogu.
        List<ServerResponseDTO> responses = serverResponses.stream()
                .filter(response -> response.getState() == State.STARTING)
                .toList();

        if (responses.isEmpty()) {
            Log.infof("Not enough information to be able to delete the environment. Correlation id: %s", correlationId);
            return;
        }

        ServerResponseDTO last = responses.get(responses.size() - 1);
        EnvironmentCreateResponse environmentCreateResponse = objectMapper
                .convertValue(last.getBody(), EnvironmentCreateResponse.class);

        EnvironmentDriverCreateDTO dto = objectMapper
                .convertValue(stopRequest.getPayload(), EnvironmentDriverCreateDTO.class);
        EnvironmentDriver environmentDriver = environmentDriverProducer
                .getEnvironmentDriver(dto.getEnvironmentDriverUrl());

        environmentDriver.cancel(environmentCreateResponse.getEnvironmentId());
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

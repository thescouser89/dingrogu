package org.jboss.pnc.dingrogu.restadapter.adapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryPromoteRequest;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryPromoteResult;
import org.jboss.pnc.common.log.ProcessStageUtils;
import org.jboss.pnc.dingrogu.api.dto.adapter.ProcessStage;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverPromoteDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.RepositoryDriverClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

@ApplicationScoped
public class RepositoryDriverPromoteAdapter implements Adapter<RepositoryDriverPromoteDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    RepositoryDriverClient repositoryDriverClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Override
    public String getAdapterName() {
        return "repository-driver-promote";
    }

    /**
     * The request to repository driver doesn't support callbacks. We'll have to simulate it!
     *
     * @param correlationId
     * @param startRequest
     */
    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {

        ProcessStageUtils.logProcessStageBegin(
                ProcessStage.COLLECTING_RESULTS_FROM_REPOSITORY_MANAGER.name(),
                "Collecting results from repository manager");
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

        RepositoryDriverPromoteDTO repoPromoteDTO = objectMapper
                .convertValue(startRequest.getPayload(), RepositoryDriverPromoteDTO.class);

        RepositoryPromoteRequest promoteRequest = RepositoryPromoteRequest.builder()
                .buildContentId(repoPromoteDTO.getBuildContentId())
                .buildType(repoPromoteDTO.getBuildType())
                .buildCategory(repoPromoteDTO.getBuildCategory())
                .tempBuild(repoPromoteDTO.isTempBuild())
                .callback(callback)
                // .heartBeat(startRequest.getHeartbeatConfig().getRequest())
                .buildConfigurationId(repoPromoteDTO.getBuildConfigurationId())
                .build();

        Log.infof("DTO for repository promote request: %s", promoteRequest);
        repositoryDriverClient.promote(repoPromoteDTO.getRepositoryDriverUrl(), promoteRequest);

        return Optional.empty();
    }

    /**
     *
     * @param correlationId
     * @param object callback object
     */
    @Override
    public void callback(String correlationId, Object object) {
        try {
            RepositoryPromoteResult response = objectMapper.convertValue(object, RepositoryPromoteResult.class);
            ProcessStageUtils.logProcessStageEnd(
                    ProcessStage.COLLECTING_RESULTS_FROM_REPOSITORY_MANAGER.name(),
                    "Collected results from repository manager.");

            try {
                if (response == null || !response.getStatus().isSuccess()) {
                    callbackEndpoint.fail(getRexTaskName(correlationId), object, null, null);
                } else {
                    Log.infof("Repository promote response: %s", response.toString());
                    callbackEndpoint.succeed(getRexTaskName(correlationId), object, null, null);
                }
            } catch (Exception e) {
                Log.error("Error happened in callback adapter", e);
            }
        } catch (IllegalArgumentException e) {
            // if we cannot cast object to AdjustResponse, it's probably a failure
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

    /**
     * Cancellation not supported by repository-driver for promotion
     *
     * @param correlationId
     * @param stopRequest
     */
    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
    }

    // @Override
    // public boolean shouldUseHeartbeat() {
    //     return true;
    // }
}

package org.jboss.pnc.dingrogu.restadapter.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.api.constants.BuildConfigurationParameterKeys;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateRequest;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateResponse;
import org.jboss.pnc.api.reqour.dto.AdjustResponse;
import org.jboss.pnc.common.log.ProcessStageUtils;
import org.jboss.pnc.dingrogu.api.dto.adapter.ProcessStage;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverSetupDTO;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.restadapter.client.RepositoryDriverClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

@ApplicationScoped
public class RepositoryDriverSetupAdapter implements Adapter<RepositoryDriverSetupDTO> {

    @Inject
    RepositoryDriverClient repositoryDriverClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    ReqourAdjustAdapter reqourAdjustAdapter;

    @Inject
    ManagedExecutor managedExecutor;

    @Override
    public String getAdapterName() {
        return "repository-driver-setup";
    }

    /**
     * The request to repository driver doesn't support callbacks. We'll have to simulate it!
     *
     * @param correlationId
     * @param startRequest
     */
    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {
        ProcessStageUtils
                .logProcessStageBegin(ProcessStage.REPO_SETTING_UP.name(), "Setting up Repository driver repository");

        Map<String, Object> pastResults = startRequest.getTaskResults();
        Object pastResult = pastResults.get(reqourAdjustAdapter.getRexTaskName(correlationId));
        AdjustResponse reqourResponse = objectMapper.convertValue(pastResult, AdjustResponse.class);

        RepositoryDriverSetupDTO repositorySetupDTO = objectMapper
                .convertValue(startRequest.getPayload(), RepositoryDriverSetupDTO.class);

        List<String> repositoriesToCreate = new ArrayList<>();
        // first add extra repositories defined in BC if any
        String paramName = BuildConfigurationParameterKeys.EXTRA_REPOSITORIES.name();
        if (repositorySetupDTO.getGenericParameters().containsKey(paramName)) {
            String extraRepos = repositorySetupDTO.getGenericParameters().get(paramName);
            extraRepos.lines().forEach(line -> {
                if (StringUtils.isNotEmpty(line)) {
                    repositoriesToCreate.add(line.trim());
                }
            });
        }
        // then add repositories removed by manipulator
        if (reqourResponse.getManipulatorResult().getRemovedRepositories() != null) {
            repositoriesToCreate.addAll(
                    reqourResponse.getManipulatorResult()
                            .getRemovedRepositories()
                            .stream()
                            .map(repo -> repo.getUrl().toString())
                            .toList());
        }

        RepositoryCreateRequest createRequest = new RepositoryCreateRequest(
                repositorySetupDTO.getBuildContentId(),
                BuildType.valueOf(repositorySetupDTO.getBuildType()),
                repositorySetupDTO.isTempBuild(),
                repositorySetupDTO.isBrewPullActive(),
                repositoriesToCreate);

        RepositoryCreateResponse response = repositoryDriverClient
                .setup(repositorySetupDTO.getRepositoryDriverUrl(), createRequest);
        ProcessStageUtils.logProcessStageEnd(
                ProcessStage.REPO_SETTING_UP.name(),
                "Repository setup complete");
        managedExecutor.submit(() -> {
            try {
                // sleep for 5 seconds to make sure that Rex has processed the successful start
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                Log.error(e);
            }
            try {
                callbackEndpoint.succeed(getRexTaskName(correlationId), response, null, null);
            } catch (Exception e) {
                Log.error("Error happened in rex client callback to Rex server for repository driver create", e);
            }
        });

        return Optional.empty();
    }

    /**
     * We're not supposed to use this since the start adapter endpoint will send the callback directly to Rex
     *
     * @param correlationId
     * @param object callback object
     */
    @Override
    public void callback(String correlationId, Object object) {
        throw new UnsupportedOperationException();
    }

    /**
     * We cannot cancel this operation since it is a synchronous one
     *
     * @param correlationId
     * @param stopRequest
     */
    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
    }

    @Override
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.BUILD_REX_NOTIFY;
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

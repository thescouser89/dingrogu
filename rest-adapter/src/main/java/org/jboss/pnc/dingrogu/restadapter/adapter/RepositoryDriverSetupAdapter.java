package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateRequest;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateResponse;
import org.jboss.pnc.api.reqour.dto.AdjustResponse;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverSetupDTO;
import org.jboss.pnc.dingrogu.restadapter.client.RepositoryDriverClient;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RepositoryDriverSetupAdapter implements Adapter<RepositoryDriverSetupDTO> {

    @Inject
    RepositoryDriverClient repositoryDriverClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RexClient rexClient;

    @Inject
    RepourAdjustAdapter repour;

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
    public void start(String correlationId, StartRequest startRequest) {

        Map<String, Object> pastResults = startRequest.getTaskResults();
        Object pastResult = pastResults.get(repour.getRexTaskName(correlationId));
        AdjustResponse reqourResponse;
        if (pastResult == null) {
            reqourResponse = rexClient.getTaskResponse(repour.getRexTaskName(correlationId), AdjustResponse.class);
        } else {
            Log.info("Obtained past response in request");
            ServerResponse serverResponse = objectMapper.convertValue(pastResult, ServerResponse.class);
            reqourResponse = objectMapper.convertValue(serverResponse.getBody(), AdjustResponse.class);
        }

        List<String> repositoriesToCreate = reqourResponse.getManipulatorResult()
                .getRemovedRepositories()
                .stream()
                .map(repo -> repo.getUrl().toString())
                .toList();

        RepositoryDriverSetupDTO repositorySetupDTO = objectMapper
                .convertValue(startRequest.getPayload(), RepositoryDriverSetupDTO.class);
        RepositoryCreateRequest createRequest = new RepositoryCreateRequest(
                repositorySetupDTO.getBuildContentId(),
                BuildType.valueOf(repositorySetupDTO.getBuildType()),
                repositorySetupDTO.isTempBuild(),
                repositorySetupDTO.isBrewPullActive(),
                repositoriesToCreate);

        RepositoryCreateResponse response = repositoryDriverClient
                .setup(repositorySetupDTO.getRepositoryDriverUrl(), createRequest);
        managedExecutor.submit(() -> {
            try {
                // sleep for 5 seconds to make sure that Rex has processed the successful start
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                Log.error(e);
            }
            try {
                rexClient.invokeSuccessCallback(getRexTaskName(correlationId), response);
            } catch (Exception e) {
                Log.error("Error happened in rex client callback to Rex server for repository driver create", e);
            }
        });
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

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
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

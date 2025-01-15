package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateRequest;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateResponse;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverSetupDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourAdjustResponse;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.RepositoryDriverClient;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    ExecutorService executorService = Executors.newFixedThreadPool(4);

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
        Log.info("----------");
        for (String taskName : pastResults.keySet()) {
            Log.info("Past result task: " + taskName);
            Log.info("Result: " + pastResults.get(taskName).toString());
            try {
                Log.info(objectMapper.convertValue(pastResults.get(taskName), RepourAdjustResponse.class));
            } catch (Exception e) {
                Log.error("Couldn't be cast to RepourAdjustResponse");
            }
            Log.info("~-~");
        }
        Log.info("----------");

        // get previous task result
        RepourAdjustResponse repourResponse = rexClient
                .getTaskResponse(repour.getRexTaskName(correlationId), RepourAdjustResponse.class);

        List<String> repositoriesToCreate = repourResponse.getRemoveRepositories();

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
        executorService.submit(() -> {
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

    @Override
    public CreateTaskDTO generateRexTask(
            String adapterUrl,
            String correlationId,
            RepositoryDriverSetupDTO repositorySetupDTO) throws Exception {

        Request startSetup = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repositorySetupDTO);

        Request cancelSetup = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repositorySetupDTO);

        return CreateTaskDTO.builder()
                .name(getRexTaskName(correlationId))
                .remoteStart(startSetup)
                .remoteCancel(cancelSetup)
                .configuration(ConfigurationDTO.builder().passResultsOfDependencies(true).build())
                .build();
    }
}

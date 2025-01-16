package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.adapter.OrchRepositoryCreationResultDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCreateRepoResponse;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.OrchClient;
import org.jboss.pnc.dto.tasks.RepositoryCreationResult;
import org.jboss.pnc.enums.ResultStatus;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class OrchRepositoryCreationResultAdapter implements Adapter<OrchRepositoryCreationResultDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    RexClient rexClient;

    @Inject
    OrchClient orchClient;

    @Inject
    RepourCreateRepositoryAdapter repourCreate;

    @Override
    public String getAdapterName() {
        return "orch-repository-creation-result";
    }

    @Override
    public void start(String correlationId, StartRequest startRequest) {

        // grab payload DTO
        OrchRepositoryCreationResultDTO dto = objectMapper
                .convertValue(startRequest.getPayload(), OrchRepositoryCreationResultDTO.class);

        Map<String, Object> pastResults = startRequest.getTaskResults();
        Object pastResult = pastResults.get(repourCreate.getRexTaskName(correlationId));
        ServerResponse serverResponse = objectMapper.convertValue(pastResult, ServerResponse.class);
        RepourCreateRepoResponse repourCreateResponse = objectMapper
                .convertValue(serverResponse.getBody(), RepourCreateRepoResponse.class);

        // generate result for Orch
        // TODO: adjust the status
        RepositoryCreationResult result = RepositoryCreationResult.builder()
                .status(ResultStatus.SUCCESS)
                .repoCreatedSuccessfully(true)
                .internalScmUrl(repourCreateResponse.getReadwriteUrl())
                .externalUrl(dto.getExternalUrl())
                .preBuildSyncEnabled(dto.isPreBuildSyncEnabled())
                .taskId(dto.getTaskId())
                .jobType(dto.getJobType())
                .buildConfiguration(dto.getBuildConfiguration())
                .build();
        orchClient.submitRepourRepositoryCreationResult(dto.getOrchUrl(), result);

        managedExecutor.submit(() -> {
            try {
                // sleep for 5 seconds to make sure that Rex has processed the successful start
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                Log.error(e);
            }
            try {
                rexClient.invokeSuccessCallback(getRexTaskName(correlationId), null);
            } catch (Exception e) {
                Log.error("Error happened in rex client callback to Rex server for orch repository create", e);
            }
        });
    }

    @Override
    public void callback(String correlationId, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateTaskDTO generateRexTask(String adapterUrl, String correlationId, OrchRepositoryCreationResultDTO dto)
            throws Exception {
        Request request = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                dto);

        Request cancelRequest = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                null);

        Request callerNotification = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getNotificationEndpoint(adapterUrl)),
                List.of(TaskHelper.getJsonHeader()),
                null);

        return CreateTaskDTO.builder()
                .name(getRexTaskName(correlationId))
                .remoteStart(request)
                .remoteCancel(cancelRequest)
                .configuration(ConfigurationDTO.builder().passResultsOfDependencies(true).build())
                .callerNotifications(callerNotification)
                .build();
    }
}

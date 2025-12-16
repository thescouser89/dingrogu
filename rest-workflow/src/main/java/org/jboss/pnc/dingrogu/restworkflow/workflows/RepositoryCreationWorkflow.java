package org.jboss.pnc.dingrogu.restworkflow.workflows;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.enums.OperationResult;
import org.jboss.pnc.api.reqour.dto.InternalSCMCreationResponse;
import org.jboss.pnc.api.reqour.dto.RepositoryCloneResponse;
import org.jboss.pnc.common.log.MDCUtils;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.adapter.ReqourCloneRepositoryDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.ReqourCreateRepositoryDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.RepositoryCreationDTO;
import org.jboss.pnc.dingrogu.common.NotificationHelper;
import org.jboss.pnc.dingrogu.restadapter.adapter.ReqourCloneRepositoryAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.ReqourCreateRepositoryAdapter;
import org.jboss.pnc.dingrogu.restadapter.client.OrchClient;
import org.jboss.pnc.dingrogu.restworkflow.workflows.helpers.WorkflowHelper;
import org.jboss.pnc.dto.tasks.RepositoryCreationResult;
import org.jboss.pnc.enums.JobNotificationType;
import org.jboss.pnc.enums.ResultStatus;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.model.requests.NotificationRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

/**
 * Implementation of the repository-creation workflow
 */
@ApplicationScoped
public class RepositoryCreationWorkflow implements Workflow<RepositoryCreationDTO> {

    @Inject
    ReqourCreateRepositoryAdapter reqourCreateRepositoryAdapter;

    @Inject
    ReqourCloneRepositoryAdapter reqourCloneRepositoryAdapter;

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    @Inject
    QueueEndpoint queueEndpoint;

    @Inject
    TaskEndpoint taskEndpoint;

    @ConfigProperty(name = "rexclient.repository_creation.queue_name")
    String rexQueueName;

    @ConfigProperty(name = "rexclient.repository_creation.queue_size")
    int rexQueueSize;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    WorkflowHelper workflowHelper;

    @Inject
    OrchClient orchClient;

    /**
     * Submit the workflow for repository-creation to Rex, and return back the correlation id
     *
     * @param repositoryCreationDTO workflow input
     * @return the correlation id
     * @throws WorkflowSubmissionException
     */
    @Override
    public CorrelationId submitWorkflow(RepositoryCreationDTO repositoryCreationDTO)
            throws WorkflowSubmissionException {
        Log.infof("Received: %s", repositoryCreationDTO);
        CorrelationId correlationId = CorrelationId.generateUnique();

        try {
            CreateGraphRequest graph = generateWorkflow(correlationId, repositoryCreationDTO);
            setRexQueueSize(queueEndpoint, rexQueueName, rexQueueSize);

            taskEndpoint.start(graph);

            return correlationId;

        } catch (Exception e) {
            throw new WorkflowSubmissionException(e);
        }
    }

    /**
     * Generates the repository creation workflow graph.
     *
     * @param correlationId
     * @param repositoryCreationDTO
     * @return
     * @throws Exception
     */
    CreateGraphRequest generateWorkflow(CorrelationId correlationId, RepositoryCreationDTO repositoryCreationDTO)
            throws Exception {
        ReqourCreateRepositoryDTO reqourCreateRepositoryDTO = ReqourCreateRepositoryDTO.builder()
                .reqourUrl(repositoryCreationDTO.getReqourUrl())
                .externalUrl(repositoryCreationDTO.getExternalRepoUrl())
                .build();

        ReqourCloneRepositoryDTO reqourCloneRepositoryDTO = ReqourCloneRepositoryDTO.builder()
                .reqourUrl(repositoryCreationDTO.getReqourUrl())
                .externalUrl(repositoryCreationDTO.getExternalRepoUrl())
                .ref(repositoryCreationDTO.getRef())
                .build();

        CreateTaskDTO taskInternalScm = reqourCreateRepositoryAdapter
                .generateRexTask(ownUrl, correlationId.getId(), repositoryCreationDTO, reqourCreateRepositoryDTO);
        CreateTaskDTO taskCloneScm = reqourCloneRepositoryAdapter
                .generateRexTask(ownUrl, correlationId.getId(), repositoryCreationDTO, reqourCloneRepositoryDTO);

        // setting up the graph
        Map<String, CreateTaskDTO> vertices = Map
                .of(taskInternalScm.name, taskInternalScm, taskCloneScm.name, taskCloneScm);

        EdgeDTO edgeDTO = EdgeDTO.builder().source(taskCloneScm.name).target(taskInternalScm.name).build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        ConfigurationDTO configurationDTO = ConfigurationDTO.builder()
                .mdcHeaderKeyMapping(MDCUtils.HEADER_KEY_MAPPING)
                .build();
        return new CreateGraphRequest(correlationId.getId(), rexQueueName, configurationDTO, edges, vertices);
    }

    @Override
    public Response rexNotification(NotificationRequest notificationRequest) {

        if (!NotificationHelper.isFromRunningToFinal(notificationRequest)) {
            // we only care about states from running to final
            return Response.ok().build();
        }

        Log.warnf(
                "[%s] -> [%s] :: %s",
                notificationRequest.getBefore(),
                notificationRequest.getAfter(),
                notificationRequest.getTask().getName());

        String correlationId = notificationRequest.getTask().getCorrelationID();
        Set<TaskDTO> tasks = taskEndpoint.byCorrelation(correlationId);

        if (NotificationHelper.areAllRexTasksInFinalState(tasks)) {
            RepositoryCreationDTO dto = objectMapper
                    .convertValue(notificationRequest.getAttachment(), RepositoryCreationDTO.class);
            Optional<InternalSCMCreationResponse> creationResponse = workflowHelper.getTaskData(
                    tasks,
                    notificationRequest.getTask().getCorrelationID(),
                    reqourCreateRepositoryAdapter,
                    InternalSCMCreationResponse.class);
            Optional<RepositoryCloneResponse> cloneResponse = workflowHelper.getTaskData(
                    tasks,
                    notificationRequest.getTask().getCorrelationID(),
                    reqourCloneRepositoryAdapter,
                    RepositoryCloneResponse.class);

            OperationResult operationResult;
            Log.infof("Creation result: %s", creationResponse);
            Log.infof("Clone result: %s", cloneResponse);

            if (creationResponse.isEmpty() || cloneResponse.isEmpty()) {
                operationResult = OperationResult.FAILED;
            } else {
                if (creationResponse.get().getCallback().getStatus().isSuccess()) {
                    operationResult = workflowHelper.toOperationResult(cloneResponse.get().getCallback().getStatus());
                } else {
                    operationResult = workflowHelper
                            .toOperationResult(creationResponse.get().getCallback().getStatus());
                }
            }

            // generate result for Orch
            // TODO replace with the one from pnc-api
            RepositoryCreationResult result = RepositoryCreationResult.builder()
                    .status(opResultToResultStatus(operationResult))
                    .repoCreatedSuccessfully(true)
                    .internalScmUrl(creationResponse.get().getReadwriteUrl())
                    .externalUrl(dto.getExternalRepoUrl())
                    .preBuildSyncEnabled(dto.isPreBuildSyncEnabled())
                    .taskId(Long.parseLong(dto.getTaskId()))
                    .jobType(convertJobType(dto.getJobNotificationType()))
                    .buildConfiguration(dto.getBuildConfiguration())
                    .build();

            Log.infof("Sending creation result back to orch: %s", result);

            orchClient.completeRepositoryCreation(dto.getOrchUrl(), result);
        }

        return Response.ok().build();
    }

    private JobNotificationType convertJobType(org.jboss.pnc.api.enums.JobNotificationType jobNotificationType) {
        return switch (jobNotificationType) {
            case BREW_PUSH -> JobNotificationType.BREW_PUSH;
            case BUILD -> JobNotificationType.BUILD;
            case BUILD_CONFIG_CREATION -> JobNotificationType.BUILD_CONFIG_CREATION;
            case GENERIC_SETTING -> JobNotificationType.GENERIC_SETTING;
            case GROUP_BUILD -> JobNotificationType.GROUP_BUILD;
            case PRODUCT_MILESTONE_CLOSE -> JobNotificationType.PRODUCT_MILESTONE_CLOSE;
            case SCM_REPOSITORY_CREATION -> JobNotificationType.SCM_REPOSITORY_CREATION;
            case OPERATION -> JobNotificationType.OPERATION;
        };
    }

    private ResultStatus opResultToResultStatus(OperationResult operationResult) {
        return switch (operationResult) {
            case SUCCESSFUL -> ResultStatus.SUCCESS;
            case FAILED -> ResultStatus.FAILED;
            case TIMEOUT -> ResultStatus.TIMED_OUT;
            case SYSTEM_ERROR -> ResultStatus.SYSTEM_ERROR;
            default -> throw new IllegalArgumentException("Unexpected value: " + operationResult);
        };
    }
}

package org.jboss.pnc.dingrogu.restworkflow.workflows;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.causeway.dto.push.PushResult;
import org.jboss.pnc.api.dto.Result;
import org.jboss.pnc.api.enums.OperationResult;
import org.jboss.pnc.common.log.MDCUtils;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.adapter.BrewPushDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.OrchBuildPushResultDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.BrewPushWorkflowDTO;
import org.jboss.pnc.dingrogu.common.NotificationHelper;
import org.jboss.pnc.dingrogu.restadapter.adapter.CausewayBuildPushAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.OrchBuildPushResultAdapter;
import org.jboss.pnc.dingrogu.restadapter.client.OrchClient;
import org.jboss.pnc.dingrogu.restworkflow.workflows.helpers.WorkflowHelper;
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
 * Implementation of the milestone-release workflow
 */
@ApplicationScoped
public class BrewPushWorkflow implements Workflow<BrewPushWorkflowDTO> {

    @Inject
    CausewayBuildPushAdapter causewayBuildPushAdapter;

    @Inject
    OrchBuildPushResultAdapter orchBuildPushResultAdapter;

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    @Inject
    TaskEndpoint taskEndpoint;

    @Inject
    QueueEndpoint queueEndpoint;

    @ConfigProperty(name = "rexclient.brew_push.queue_name")
    String rexQueueName;

    @ConfigProperty(name = "rexclient.brew_push.queue_size")
    int rexQueueSize;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    WorkflowHelper workflowHelper;

    @Inject
    OrchClient orchClient;

    @Override
    public CorrelationId submitWorkflow(BrewPushWorkflowDTO brewPushWorkflowDTO) throws WorkflowSubmissionException {
        Log.infof("DTO for submitWorkflow: %s", brewPushWorkflowDTO);

        CorrelationId correlationId = CorrelationId.generateUnique();

        BrewPushDTO brewPushDTO = BrewPushDTO.builder()
                .buildId(brewPushWorkflowDTO.getBuildId())
                .tagPrefix(brewPushWorkflowDTO.getTagPrefix())
                .username(brewPushWorkflowDTO.getUsername())
                .reimport(brewPushWorkflowDTO.isReimport())
                .causewayUrl(brewPushWorkflowDTO.getCausewayUrl())
                .build();

        OrchBuildPushResultDTO orchPushResultDTO = OrchBuildPushResultDTO.builder()
                .operationId(brewPushWorkflowDTO.getOperationId())
                .orchUrl(brewPushWorkflowDTO.getOrchUrl())
                .buildId(brewPushWorkflowDTO.getBuildId())
                .build();

        try {

            CreateTaskDTO buildPush = causewayBuildPushAdapter
                    .generateRexTask(ownUrl, correlationId.getId(), brewPushWorkflowDTO, brewPushDTO);

            CreateTaskDTO buildPushResult = orchBuildPushResultAdapter
                    .generateRexTask(ownUrl, correlationId.getId(), brewPushWorkflowDTO, orchPushResultDTO);

            Map<String, CreateTaskDTO> vertices = Map
                    .of(buildPush.name, buildPush, buildPushResult.name, buildPushResult);

            EdgeDTO edgeDTO = EdgeDTO.builder().source(buildPushResult.name).target(buildPush.name).build();
            Set<EdgeDTO> edges = Set.of(edgeDTO);

            ConfigurationDTO configurationDTO = ConfigurationDTO.builder()
                    .mdcHeaderKeyMapping(MDCUtils.HEADER_KEY_MAPPING)
                    .build();

            CreateGraphRequest graphRequest = new CreateGraphRequest(
                    correlationId.getId(),
                    rexQueueName,
                    configurationDTO,
                    edges,
                    vertices);
            setRexQueueSize(queueEndpoint, rexQueueName, rexQueueSize);
            taskEndpoint.start(graphRequest);

            return correlationId;
        } catch (Exception e) {
            throw new WorkflowSubmissionException(e);
        }
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
            BrewPushWorkflowDTO dto = objectMapper
                    .convertValue(notificationRequest.getAttachment(), BrewPushWorkflowDTO.class);
            Optional<PushResult> push = workflowHelper.getTaskData(
                    tasks,
                    notificationRequest.getTask().getCorrelationID(),
                    causewayBuildPushAdapter,
                    PushResult.class);
            Optional<Result> pushOrchResult = workflowHelper.getTaskData(
                    tasks,
                    notificationRequest.getTask().getCorrelationID(),
                    orchBuildPushResultAdapter,
                    Result.class);

            OperationResult operationResult;
            Log.infof("Brew push result: %s", push);
            Log.infof("Orch push result: %s", pushOrchResult);

            if (push.isEmpty() || pushOrchResult.isEmpty()) {
                operationResult = OperationResult.FAILED;
            } else {
                if (push.get().getResult().isSuccess() && pushOrchResult.get().getResult().isSuccess()) {
                    operationResult = OperationResult.SUCCESSFUL;
                } else {
                    operationResult = workflowHelper.toOperationResult(push.get().getResult());
                }
            }

            orchClient.completeOperation(dto.getOrchUrl(), operationResult, dto.getOperationId());
        }

        return Response.ok().build();
    }
}
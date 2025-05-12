package org.jboss.pnc.dingrogu.restworkflow.workflows;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalysisReport;
import org.jboss.pnc.api.dto.Result;
import org.jboss.pnc.api.enums.OperationResult;
import org.jboss.pnc.common.log.MDCUtils;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.adapter.DeliverablesAnalyzerDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.OrchDeliverablesAnalyzerResultDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.DeliverablesAnalysisWorkflowDTO;
import org.jboss.pnc.dingrogu.common.NotificationHelper;
import org.jboss.pnc.dingrogu.restadapter.adapter.DeliverablesAnalyzerAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.OrchDeliverablesAnalyzerResultAdapter;
import org.jboss.pnc.dingrogu.restadapter.client.GenericClient;
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
 * Deliverables analysis workflow implementation
 */
@ApplicationScoped
public class DeliverablesAnalysisWorkflow implements Workflow<DeliverablesAnalysisWorkflowDTO> {

    @Inject
    TaskEndpoint taskEndpoint;

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    @Inject
    DeliverablesAnalyzerAdapter deliverablesAnalyzerAdapter;

    @Inject
    OrchDeliverablesAnalyzerResultAdapter orchAdapter;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    GenericClient genericClient;

    @Inject
    QueueEndpoint queueEndpoint;

    @Inject
    WorkflowHelper workflowHelper;

    @Inject
    OrchClient orchClient;

    @ConfigProperty(name = "rexclient.deliverables_analysis.queue_name")
    String rexQueueName;

    @ConfigProperty(name = "rexclient.deliverables_analysis.queue_size")
    int rexQueueSize;

    @Override
    public CorrelationId submitWorkflow(DeliverablesAnalysisWorkflowDTO dto) throws WorkflowSubmissionException {
        Log.infof("DTO for submitWorkflow: %s", dto);

        CorrelationId correlationId = CorrelationId.generateUnique();

        DeliverablesAnalyzerDTO delaDTO = DeliverablesAnalyzerDTO.builder()
                .operationId(dto.getOperationId())
                .urls(dto.getUrls())
                .config(dto.getConfig())
                .deliverablesAnalyzerUrl(dto.getDeliverablesAnalyzerUrl())
                .build();

        OrchDeliverablesAnalyzerResultDTO orchResultDTO = OrchDeliverablesAnalyzerResultDTO.builder()
                .operationId(dto.getOperationId())
                .scratch(dto.isScratch())
                .orchUrl(dto.getOrchUrl())
                .build();

        try {
            CreateTaskDTO taskAnalyze = deliverablesAnalyzerAdapter
                    .generateRexTaskRetryItself(ownUrl, correlationId.getId(), dto, delaDTO);

            CreateTaskDTO taskResult = orchAdapter.generateRexTask(ownUrl, correlationId.getId(), dto, orchResultDTO);

            Map<String, CreateTaskDTO> vertices = Map.of(taskAnalyze.name, taskAnalyze, taskResult.name, taskResult);

            EdgeDTO edgeDTO = EdgeDTO.builder().source(taskResult.name).target(taskAnalyze.name).build();
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

    /**
     * We ran both the deliverables analysis and the sending result to orch. As a final step, we need to tell PNC that
     * the "operation is done". We'll use the final notification part to tell Orch about the status of the operation
     *
     * @param notificationRequest
     * @return
     */
    @Override
    public Response rexNotification(NotificationRequest notificationRequest) {

        if (!NotificationHelper.isFromRunningToFinal(notificationRequest)) {
            // we only care about states from running to final
            return Response.ok().build();
        }

        Log.infof(
                "[%s] -> [%s] :: %s",
                notificationRequest.getBefore(),
                notificationRequest.getAfter(),
                notificationRequest.getTask().getName());

        String correlationId = notificationRequest.getTask().getCorrelationID();
        Set<TaskDTO> tasks = taskEndpoint.byCorrelation(correlationId);

        if (NotificationHelper.areAllRexTasksInFinalState(tasks)) {

            Log.infof("Right now I should be sending a notification to the caller");
            tasks.forEach(taskDTO -> Log.infof("Task: %s, state: %s", taskDTO.getName(), taskDTO.getState()));

            // we set the notification attachment to be the StartRequest in submitWorkflow method
            DeliverablesAnalysisWorkflowDTO dto = objectMapper
                    .convertValue(notificationRequest.getAttachment(), DeliverablesAnalysisWorkflowDTO.class);
            Optional<AnalysisReport> analysis = workflowHelper.getTaskData(
                    tasks,
                    notificationRequest.getTask().getCorrelationID(),
                    deliverablesAnalyzerAdapter,
                    AnalysisReport.class);
            Optional<Result> orchResultSender = workflowHelper
                    .getTaskData(tasks, notificationRequest.getTask().getCorrelationID(), orchAdapter, Result.class);

            OperationResult operationResult;
            Log.infof("Orch result: %s", orchResultSender);
            Log.infof("Analysis result: %s", analysis);
            if (analysis.isEmpty() || orchResultSender.isEmpty()) {
                operationResult = OperationResult.FAILED;
            } else {
                if (!analysis.get().isSuccess()) {
                    operationResult = OperationResult.FAILED;
                } else {
                    operationResult = workflowHelper.toOperationResult(orchResultSender.get().getResult());
                }
            }

            orchClient.completeOperation(dto.getOrchUrl(), operationResult, dto.getOperationId());
        }
        return Response.ok().build();
    }
}

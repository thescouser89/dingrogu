package org.jboss.pnc.dingrogu.restworkflow.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.common.log.MDCUtils;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.adapter.DeliverablesAnalyzerDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.OrchDeliverablesAnalyzerResultDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.DeliverablesAnalysisWorkflowDTO;
import org.jboss.pnc.dingrogu.restadapter.adapter.DeliverablesAnalyzerAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.OrchDeliverablesAnalyzerResultAdapter;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.util.Map;
import java.util.Set;

/**
 * Deliverables analysis workflow implementation
 */
@ApplicationScoped
public class DeliverablesAnalysisWorkflow implements Workflow<DeliverablesAnalysisWorkflowDTO> {

    @Inject
    RexClient rexClient;

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    @Inject
    DeliverablesAnalyzerAdapter deliverablesAnalyzerAdapter;

    @Inject
    OrchDeliverablesAnalyzerResultAdapter orchAdapter;

    @Override
    public CorrelationId submitWorkflow(DeliverablesAnalysisWorkflowDTO dto) throws WorkflowSubmissionException {

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
                    .generateRexTask(ownUrl, correlationId.getId(), null, delaDTO);

            CreateTaskDTO taskResult = orchAdapter.generateRexTask(ownUrl, correlationId.getId(), null, orchResultDTO);

            Map<String, CreateTaskDTO> vertices = Map.of(taskAnalyze.name, taskAnalyze, taskResult.name, taskResult);

            EdgeDTO edgeDTO = EdgeDTO.builder().source(taskResult.name).target(taskAnalyze.name).build();
            Set<EdgeDTO> edges = Set.of(edgeDTO);

            ConfigurationDTO configurationDTO = ConfigurationDTO.builder()
                    .mdcHeaderKeyMapping(MDCUtils.HEADER_KEY_MAPPING)
                    .build();
            CreateGraphRequest graphRequest = new CreateGraphRequest(
                    correlationId.getId(),
                    configurationDTO,
                    edges,
                    vertices);
            rexClient.submitWorkflow(graphRequest);

            return correlationId;

        } catch (Exception e) {
            throw new WorkflowSubmissionException(e);
        }
    }
}

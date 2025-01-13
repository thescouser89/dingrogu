package org.jboss.pnc.dingrogu.restworkflow.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.workflow.DeliverablesAnalysisWorkflowDTO;
import org.jboss.pnc.dingrogu.restadapter.adapter.DeliverablesAnalyzerAdapter;
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

    @Override
    public CorrelationId submitWorkflow(DeliverablesAnalysisWorkflowDTO dto) throws WorkflowSubmissionException {

        CorrelationId correlationId = CorrelationId.generateUnique();

        try {
            CreateTaskDTO task = deliverablesAnalyzerAdapter
                    .generateRexTask(ownUrl, correlationId.getId(), dto.getDeliverablesAnalyzer());

            Map<String, CreateTaskDTO> vertices = Map.of(task.name, task);

            // EdgeDTO edgeDTO = EdgeDTO.builder().source(task.name).target(null).build();
            // Set<EdgeDTO> edges = Set.of(edgeDTO);
            Set<EdgeDTO> edges = Set.of();

            CreateGraphRequest graphRequest = new CreateGraphRequest(correlationId.getId(), null, edges, vertices);
            rexClient.submitWorkflow(graphRequest);

            return correlationId;

        } catch (Exception e) {
            throw new WorkflowSubmissionException(e);
        }
    }
}

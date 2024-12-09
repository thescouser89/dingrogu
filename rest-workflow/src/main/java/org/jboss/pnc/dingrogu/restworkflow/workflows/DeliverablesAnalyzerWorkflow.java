package org.jboss.pnc.dingrogu.restworkflow.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalyzePayload;
import org.jboss.pnc.dingrogu.restworkflow.tasks.DeliverablesAnalyzerTask;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.util.Map;
import java.util.Set;

/**
 * Generate the entire Rex workflow for deliverables analyzer analysis
 */
@ApplicationScoped
public class DeliverablesAnalyzerWorkflow {

    public static final String DELIVERABLES_ANALYZER_KEY = "deliverables-analyzer:";

    @Inject
    DeliverablesAnalyzerTask deliverablesAnalyzerTask;

    public CreateGraphRequest generateWorkflow(AnalyzePayload analyzePayload) throws Exception {
        // TODO: add notification to Orch
        CreateTaskDTO analyzeTask = deliverablesAnalyzerTask.getTask(analyzePayload);

        Map<String, CreateTaskDTO> vertices = Map.of(analyzeTask.name, analyzeTask);

        EdgeDTO edgeDTO = EdgeDTO.builder().source(analyzeTask.name).target(null).build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        return new CreateGraphRequest(DELIVERABLES_ANALYZER_KEY + "::" + analyzePayload.getOperationId(), null, edges, vertices);
    }
}

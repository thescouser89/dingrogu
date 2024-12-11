package org.jboss.pnc.dingrogu.restworkflow.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.workflow.DeliverablesAnalysisWorkDTO;

/**
 * Deliverables analysis workflow implementation
 */
@ApplicationScoped
public class DeliverablesAnalysisWorkflow implements Workflow<DeliverablesAnalysisWorkDTO> {

    @Override
    public CorrelationId submitWorkflow(DeliverablesAnalysisWorkDTO deliverablesAnalysisWorkDTO)
            throws WorkflowSubmissionException {
        // TODO: implement
        throw new UnsupportedOperationException();
    }
}

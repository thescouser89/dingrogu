package org.jboss.pnc.dingrogu.restworkflow.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildWorkDTO;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;

/**
 * Build process workflow implementation
 */
@ApplicationScoped
public class BuildWorkflow implements Workflow<BuildWorkDTO> {

    @Override
    public CorrelationId submitWorkflow(BuildWorkDTO buildWorkDTO) throws WorkflowSubmissionException {
        // TODO: implement
        throw new UnsupportedOperationException();
    }
}

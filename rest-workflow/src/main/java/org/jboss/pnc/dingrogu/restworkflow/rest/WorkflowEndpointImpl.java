package org.jboss.pnc.dingrogu.restworkflow.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildWorkDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.DeliverablesAnalysisWorkDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.BrewPushDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.RepositoryCreationDTO;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.restworkflow.workflows.BuildWorkflow;
import org.jboss.pnc.dingrogu.restworkflow.workflows.DeliverablesAnalysisWorkflow;
import org.jboss.pnc.dingrogu.restworkflow.workflows.BrewPushWorkflow;
import org.jboss.pnc.dingrogu.restworkflow.workflows.DummyWorkflow;
import org.jboss.pnc.dingrogu.restworkflow.workflows.RepositoryCreationWorkflow;

/**
 * Implementation of the workflow endpoint
 *
 * Any new workflow will require the addition of a new endpoint here
 */
@ApplicationScoped
public class WorkflowEndpointImpl implements WorkflowEndpoint {

    @Inject
    BrewPushWorkflow brewPushWorkflow;

    @Inject
    RepositoryCreationWorkflow repositoryCreationWorkflow;

    @Inject
    BuildWorkflow buildWorkflow;

    @Inject
    DeliverablesAnalysisWorkflow deliverablesAnalysisWorkflow;

    @Inject
    DummyWorkflow dummyWorkflow;

    @Override
    public CorrelationId startBrewPushWorkflow(BrewPushDTO brewPushDTO) {
        return brewPushWorkflow.submitWorkflow(brewPushDTO);
    }

    @Override
    public CorrelationId startRepositoryCreationWorkflow(RepositoryCreationDTO repositoryCreationDTO) {
        return repositoryCreationWorkflow.submitWorkflow(repositoryCreationDTO);
    }

    @Override
    public CorrelationId startBuildWorkflow(BuildWorkDTO buildWorkDTO) {
        return buildWorkflow.submitWorkflow(buildWorkDTO);
    }

    @Override
    public CorrelationId startDeliverablesAnalysisWorkflow(DeliverablesAnalysisWorkDTO deliverablesAnalysisWorkDTO) {
        return deliverablesAnalysisWorkflow.submitWorkflow(deliverablesAnalysisWorkDTO);
    }

    @Override
    public CorrelationId startDummyWorkflow(Object object) {
        return dummyWorkflow.submitWorkflow(object);
    }

    public Response cancelWorkflow(String correlationId) {
        // TODO: interact with Rex to submit cancel of the graph, and return an error for not found or rex not
        // responding
        throw new UnsupportedOperationException();
    }
}

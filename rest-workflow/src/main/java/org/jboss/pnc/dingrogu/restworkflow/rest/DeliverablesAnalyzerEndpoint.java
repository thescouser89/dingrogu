package org.jboss.pnc.dingrogu.restworkflow.rest;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalyzePayload;
import org.jboss.pnc.dingrogu.api.Endpoints;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.restworkflow.workflows.DeliverablesAnalyzerWorkflow;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DeliverablesAnalyzerEndpoint {

    @Inject
    DeliverablesAnalyzerWorkflow workflow;

    @Inject
    RexClient rexClient;

    @POST
    @Path(Endpoints.WORKFLOW_DELIVERABLES_ANALYZER_ANALYZE_START)
    public Response startDeliverablesAnalyzer(AnalyzePayload analyzePayload) throws Exception {
        CreateGraphRequest graphRequest = workflow.generateWorkflow(analyzePayload);
        return Response.ok().build();
    }


}

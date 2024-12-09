package org.jboss.pnc.dingrogu.restadapter.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.pnc.dingrogu.api.Endpoints;
import org.jboss.pnc.rex.model.requests.StartRequest;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DeliverablesAnalyzerAdapterEndpoint {

    @POST
    @Path(Endpoints.ADAPTER_DELIVERABLES_ANALYZER_ANALYZE_START)
    public String startAnalysis(StartRequest request) {
        return null;
    }

    @POST
    @Path(Endpoints.ADAPTER_DELIVERABLES_ANALYZER_ANALYZE_CANCEL)
    public String cancelAnalysis(StartRequest request) {
        return null;
    }

    @POST
    @Path(Endpoints.ADAPTER_DELIVERABLES_ANALYZER_ANALYZE_CALLBACK)
    public String callbackAnalysis() {
        return null;
    }
}

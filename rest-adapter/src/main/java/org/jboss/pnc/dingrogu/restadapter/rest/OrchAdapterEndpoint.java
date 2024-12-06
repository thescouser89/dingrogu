package org.jboss.pnc.dingrogu.restadapter.rest;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.rex.model.requests.StartRequest;

@Path("/adapter/orch")
public class OrchAdapterEndpoint {

    @Path("/notify-orch-of-causeway")
    public Response notifyOrchOfCauseway(StartRequest request) {
        return null;
    }
}

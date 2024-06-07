package org.jboss.pnc.dingrogu.rest.adapter;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.rex.model.requests.StartRequest;

@Path("/adapter/orch")
public class OrchAdapterEndpoint {

    @Path("/notify-orch-of-causeway")
    public Response notifyOrchOfCauseway(StartRequest request) {

    }
}

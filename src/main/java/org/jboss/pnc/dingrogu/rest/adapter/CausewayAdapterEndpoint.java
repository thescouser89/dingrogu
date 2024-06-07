package org.jboss.pnc.dingrogu.rest.adapter;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.api.dto.MilestoneReleaseRequest;
import org.jboss.pnc.rex.model.requests.StartRequest;

@Path("/adapter/causeway")
public class CausewayAdapterEndpoint {

    @Path("import-milestone")
    @POST
    public Response importMilestone(StartRequest request) {

        // TODO: is milestoneId a string or an id like in the Causeway DTO?
        String milestoneId = (String) request.getPayload();

        MilestoneReleaseRequest milestoneReleaseRequest = MilestoneReleaseRequest.builder()
                .callback(request.getPositiveCallback())
                .milestoneId(milestoneId)
                .build();

        // TODO: handle any MDC values
        // TODO: POST to causeway
        // TODO: handle authentication: passthrough the token
        // TODO send response of POST to caller

        return Response.accepted().build();
    }
}

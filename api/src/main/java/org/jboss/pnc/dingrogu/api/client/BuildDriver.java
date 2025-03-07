package org.jboss.pnc.dingrogu.api.client;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.pnc.api.builddriver.dto.BuildCancelRequest;
import org.jboss.pnc.api.builddriver.dto.BuildRequest;
import org.jboss.pnc.api.builddriver.dto.BuildResponse;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface BuildDriver {

    @POST
    @Path("/build")
    CompletionStage<BuildResponse> build(BuildRequest buildRequest);

    @PUT
    @Path("/cancel")
    CompletionStage<Response> cancel(BuildCancelRequest buildCancelRequest);
}

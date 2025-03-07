package org.jboss.pnc.dingrogu.api.client;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCompleteRequest;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCompleteResponse;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateRequest;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateResponse;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface EnvironmentDriver {

    @POST
    @Path("/create")
    CompletionStage<EnvironmentCreateResponse> build(EnvironmentCreateRequest environmentCreateRequest);

    @PUT
    @Path("/complete")
    CompletionStage<EnvironmentCompleteResponse> complete(EnvironmentCompleteRequest environmentCompleteRequest);

    @PUT
    @Path("/cancel/{environmentId}")
    CompletionStage<EnvironmentCompleteResponse> cancel(@PathParam("environmentId") String environmentId);
}

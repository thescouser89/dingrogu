package org.jboss.pnc.dingrogu.rest.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import org.jboss.pnc.rex.api.parameters.TaskFilterParameters;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import javax.ws.rs.BeanParam;
import java.util.Set;

/**
 * Copy-Pastaed from Rex's TaskEndpoint but using jakarta annotations instead of javax.
 */
@Path("/rest/tasks")
public interface RexTaskEndpoint {
    @POST
    @Produces({ "application/json" })
    @Consumes({ "application/json" })
    Set<TaskDTO> start(@Valid @NotNull CreateGraphRequest request);

    @GET
    @Produces({ "application/json" })
    Set<TaskDTO> getAll(@BeanParam TaskFilterParameters filterParameters);

    @Path("/{taskID}")
    @GET
    @Produces({ "application/json" })
    TaskDTO getSpecific(@PathParam("taskID") @NotBlank String taskID);

    @Path("/{taskID}/cancel")
    @PUT
    void cancel(@PathParam("taskID") @NotBlank String taskID);

    @GET
    @Path("/by-correlation/{correlationID}")
    @Produces({ "application/json" })
    Set<TaskDTO> byCorrelation(@PathParam("correlationID") @NotBlank String correlationID);
}

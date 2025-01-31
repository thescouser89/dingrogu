package org.jboss.pnc.dingrogu.api.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.api.konfluxbuilddriver.dto.BuildRequest;
import org.jboss.pnc.api.konfluxbuilddriver.dto.BuildResponse;
import org.jboss.pnc.api.konfluxbuilddriver.dto.CancelRequest;

/**
 * Author: Tim Carter (tecarter94)
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface KonfluxBuildDriver {

    @POST
    @Path("/build")
    BuildResponse build(BuildRequest buildRequest);

    @PUT
    @Path("/cancel")
    Response cancel(CancelRequest cancelRequest);
}

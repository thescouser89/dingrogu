package org.jboss.pnc.dingrogu.rest.adapter;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;


/**
 * The RepourAdapterEndpoint is used to translate Rex's StartRequest and StopRequest to Repour APIs
 */
@Path("/adapter/repour")
public class RepourAdapterEndpoint {

    /**
     * Send a request to create an internal repository for Repour.
     * Repour doesn't require a callback for this endpoint, so the response contains the result from Repour
     *
     * @param request request DTO from Rex
     * @return whether the request to Repour was successful or not
     */
    @Path("create-repository-start")
    @POST
    public Response createInternalRepo(StartRequest request) {
        return null;
    }

    /**
     * Send a request to clone from an external repository to internal repository for Repour
     *
     * @param request request DTO from Rex
     * @return whether the initial request to Repour was successful or not
     */
    @Path("clone-repository-start")
    @POST
    public Response cloneRepositoryStart(StartRequest request) {
        return null;
    }

    /**
     * Send a request to stop / cancel the cloning from an external repository to internal repository for Repour
     *
     * @param request request DTO from Rex
     * @return whether the stop request to Repour was successful or not
     */
    @Path("clone-repository-stop")
    @POST
    public Response cloneRepositoryStop(StopRequest request) {
        return null;
    }
}

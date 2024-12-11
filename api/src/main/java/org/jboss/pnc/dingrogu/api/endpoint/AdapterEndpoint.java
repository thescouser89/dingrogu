package org.jboss.pnc.dingrogu.api.endpoint;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

/**
 * Adapter endpoint interface. We separate interface and implementation in case we want to generate a REST client from
 * the interface
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public interface AdapterEndpoint {

    String START = "/adapter/{name}/{correlationId}/start";
    String CANCEL = "/adapter/{name}/{correlationId}/cancel";
    String CALLBACK = "/adapter/{name}/{correlationId}/callback";

    static String getStartAdapterEndpoint(String dingroguUrl, String name, String correlationId) {
        String start = dingroguUrl + AdapterEndpoint.START;
        return start.replace("{name}", name).replace("{correlationId}", correlationId);
    }

    static String getCancelAdapterEndpoint(String dingroguUrl, String name, String correlationId) {
        String start = dingroguUrl + AdapterEndpoint.CANCEL;
        return start.replace("{name}", name).replace("{correlationId}", correlationId);
    }

    static String getCallbackAdapterEndpoint(String dingroguUrl, String name, String correlationId) {
        String start = dingroguUrl + AdapterEndpoint.CALLBACK;
        return start.replace("{name}", name).replace("{correlationId}", correlationId);
    }

    /**
     * Get the Rex task request and forward it to the application
     *
     * @param name name of the adapter
     * @param correlationId correlation id of the Rex task
     * @param startRequest Rex DTO
     * @return response
     */
    @Path(START)
    @POST
    Response start(
            @PathParam("name") String name,
            @PathParam("correlationId") String correlationId,
            StartRequest startRequest);

    /**
     * Get the Rex task cancel request and forward it to the application
     *
     * @param name name of the adapter
     * @param correlationId correlation id of the Rex task
     * @param stopRequest Rex DTO
     * @return response
     */
    @Path(CANCEL)
    @POST
    Response cancel(
            @PathParam("name") String name,
            @PathParam("correlationId") String correlationId,
            StopRequest stopRequest);

    /**
     * Get the callback from the application and forward it to the Rex task
     *
     * @param name name of the adapter
     * @param correlationId correlation id of the Rex task
     * @param object callback object
     * @return response
     */
    @Path(CALLBACK)
    @POST
    Response callback(@PathParam("name") String name, @PathParam("correlationId") String correlationId, Object object);
}

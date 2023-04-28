package org.jboss.pnc.dingrogu.rest;

import io.quarkus.logging.Log;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.dingrogu.GenerateTask;
import org.jboss.pnc.rex.api.TaskEndpoint;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @Inject
    @RestClient
    TaskEndpoint taskEndpoint;

    @Inject
    GenerateTask generateTask;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from RESTEasy Reactive";
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String startProcess() throws Exception {
        taskEndpoint.start(generateTask.generateSingleRequest());
        Log.info("Request started");
        return "oh hey";
    }
}
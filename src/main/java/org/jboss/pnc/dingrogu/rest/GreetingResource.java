package org.jboss.pnc.dingrogu.rest;

import io.quarkus.logging.Log;
import io.quarkus.runtime.annotations.ConfigItem;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.dingrogu.GenerateTask;
import org.jboss.pnc.dingrogu.rest.client.RexClient;
import org.jboss.pnc.rex.api.TaskEndpoint;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Path("/hello")
public class GreetingResource {

    @Inject
    GenerateTask generateTask;

    @ConfigItem(name = "rex_api_url")
    String rexClientUrl;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from RESTEasy Reactive";
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String startProcess() throws Exception {
        Log.info("Rex client url is: " + rexClientUrl);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(rexClientUrl)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        RexClient rexClient = retrofit.create(RexClient.class);
        rexClient.start(generateTask.generateSingleRequest());
        Log.info("Request started");
        return "oh hey";
    }
}
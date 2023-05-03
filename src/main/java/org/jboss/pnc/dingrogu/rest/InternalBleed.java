package org.jboss.pnc.dingrogu.rest;


import io.quarkus.logging.Log;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import org.jboss.pnc.dingrogu.GenerateTask;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.concurrent.CompletableFuture;
import jakarta.ws.rs.core.MediaType;

@Path("receive-from-rex")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InternalBleed {

    @Inject
    GenerateTask generateTask;

    @POST
    @Path("/start")
    public String getRequestStart(StartRequest startRequest) {
        CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Log.info("oh lala : " + e);
            }
            Log.info("Sending callback for start request");
            generateTask.callbackReply(true, "all good");
            return null;
        });

        Log.info("[Request Start] Callback url is: " + startRequest.getCallback());
        return "ok, fine";
    }

    @POST
    @Path("/cancel")
    public String getCancelRequest(StopRequest stopRequest) {

        CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Log.info("oh lala : " + e);
            }
            Log.info("Sending callback for cancel request");
            generateTask.callbackReply(true, "all good");
            return null;
        });

        Log.info("[Cancel Request] Callback url is: " + stopRequest.getCallback());
        return "ok, fine";
    }
}

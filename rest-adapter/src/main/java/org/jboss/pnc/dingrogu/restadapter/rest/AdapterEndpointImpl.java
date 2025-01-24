package org.jboss.pnc.dingrogu.restadapter.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.All;
import io.quarkus.logging.Log;
import io.quarkus.oidc.client.Tokens;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import kong.unirest.core.ContentType;
import kong.unirest.core.Unirest;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.restadapter.adapter.Adapter;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.requests.MinimizedTask;
import org.jboss.pnc.rex.model.requests.NotificationRequest;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter endpoint: Each Rex task will call the Adapter endpoint for that task. The endpoint will translate the Rex DTO
 * to the application's DTO and submit the request to the application for start, cancel, and accept the callback from
 * the app to send to Rex
 *
 * Addition of another Rex task/adapter shouldn't require to add a new endpoint to this class. The new adapter should be
 * auto-discovered automatically
 */
@ApplicationScoped
public class AdapterEndpointImpl implements AdapterEndpoint {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Tokens tokens;

    @Inject
    TaskEndpoint taskEndpoint;

    /**
     * Get all the implementations of the Adapter interface
     */
    @Inject
    @All
    List<Adapter<?>> adapters;

    private final Map<String, Adapter<?>> adapterNameMap = new HashMap<>();

    /**
     * Populate the adapterNameMap with as key the name of the adapter, and as value the adapter itself
     */
    @PostConstruct
    public void setup() {

        for (Adapter<?> adapter : adapters) {
            adapterNameMap.put(adapter.getAdapterName(), adapter);
        }
    }

    @Override
    public Response start(String name, String correlationId, StartRequest startRequest) {
        Log.infof("Start adapter for: '%s' with correlation-id: '%s'", name, correlationId);

        Adapter<?> adapter = adapterNameMap.get(name);

        if (adapter == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        adapter.start(correlationId, startRequest);
        return Response.accepted().build();
    }

    @Override
    public Response cancel(String name, String correlationId, StopRequest stopRequest) {
        Log.infof("Cancel adapter for: '%s' with correlation-id: '%s'", name, correlationId);

        Adapter<?> adapter = adapterNameMap.get(name);

        if (adapter == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        adapter.cancel(correlationId, stopRequest);
        return Response.accepted().build();
    }

    @Override
    public Response callback(String name, String correlationId, Object object) {
        Log.infof("Callback adapter for: '%s' with correlation-id: '%s'", name, correlationId);

        Adapter<?> adapter = adapterNameMap.get(name);

        if (adapter == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        adapter.callback(correlationId, object);
        return Response.ok().build();
    }

    // TODO: MDC
    // TODO: remove it now that Workflow handles the rexNotification?
    @Override
    public Response rexNotification(NotificationRequest notificationRequest) {

        MinimizedTask task = notificationRequest.getTask();

        /**
         * from {@link Adapter#generateRexTask(String, String, Request, Object)}, we want the attachment part to be a
         * Request object, so that we can forward the failed notification to Request
         */
        Request attachment = objectMapper.convertValue(notificationRequest.getAttachment(), Request.class);
        State stateBefore = notificationRequest.getBefore();
        State stateAfter = notificationRequest.getAfter();
        Log.infof(
                "[%s -> %s] Correlation: %s, Task: %s",
                stateBefore,
                stateAfter,
                task.getCorrelationID(),
                task.getName());

        if (stateAfter.isFinal()) {
            Set<TaskDTO> tasks = taskEndpoint.byCorrelation(notificationRequest.getTask().getCorrelationID());
            tasks.forEach(taskDTO -> Log.infof("Task present in notification: %s", taskDTO.getName()));
        }

        // only send the request in the attachment for a failure notification
        if (stateAfter.isFinal() && stateAfter.toString().toLowerCase().contains("fail") && attachment != null) {
            Log.info("State failed: sending notification request from attachment");

            Map<String, String> headerMap = new HashMap<>();
            List<Request.Header> headers = attachment.getHeaders();

            headerMap.put("Authorization", "Bearer " + tokens.getAccessToken());

            if (headers != null) {
                for (Request.Header header : headers) {
                    headerMap.put(header.getName(), header.getValue());
                }
            }

            List<ServerResponse> serverResponses = task.getServerResponses();
            ServerResponse lastResponse = serverResponses.get(serverResponses.size() - 1);

            // Send request to notify that the process failed
            Unirest.post(attachment.getUri().toString())
                    .contentType(ContentType.APPLICATION_JSON)
                    .accept(ContentType.APPLICATION_JSON)
                    .headers(headerMap)
                    .body(lastResponse)
                    .asEmpty();
        }
        return Response.ok().build();
    }
}
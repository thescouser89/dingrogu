package org.jboss.pnc.dingrogu.restadapter.rest;

import io.quarkus.arc.All;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.restadapter.adapter.Adapter;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.model.requests.MinimizedTask;
import org.jboss.pnc.rex.model.requests.NotificationRequest;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Override
    public Response rexNotification(NotificationRequest notificationRequest) {

        MinimizedTask task = notificationRequest.getTask();
        Object attachment = notificationRequest.getAttachment();
        State stateBefore = notificationRequest.getBefore();
        State stateAfter = notificationRequest.getAfter();
        Log.infof(
                "[%s -> %s] Correlation: %s, Task: %s",
                stateBefore,
                stateAfter,
                task.getCorrelationID(),
                task.getName());

        if (stateAfter.isFinal() && stateAfter.toString().toLowerCase().contains("fail")) {
            Log.info("State failed!");
        }
        return Response.ok().build();
    }
}
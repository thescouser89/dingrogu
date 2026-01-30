package org.jboss.pnc.dingrogu.restadapter.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.restadapter.adapter.Adapter;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.quarkus.arc.All;
import io.quarkus.logging.Log;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;

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
            Log.infof(">>> Processing adapter: %s", adapter.getAdapterName());
            adapterNameMap.put(adapter.getAdapterName(), adapter);
        }
    }

    /**
     * Needed so that any runtime exception from the REST client is mapped and the message is sent to the caller
     *
     * @param e
     * @return
     */
    @ServerExceptionMapper
    public RestResponse<String> mapException(RuntimeException e) {
        if (e instanceof UnauthorizedException) {
            return RestResponse.status(Response.Status.UNAUTHORIZED, e.getMessage());
        } else if (e instanceof ForbiddenException) {
            return RestResponse.status(Response.Status.FORBIDDEN, e.getMessage());
        } else {
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public Response start(String name, String correlationId, StartRequest startRequest) {
        Log.infof("Start adapter for: '%s' with correlation-id: '%s'", name, correlationId);

        Adapter<?> adapter = adapterNameMap.get(name);

        if (adapter == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            Optional<Object> response = adapter.start(correlationId, startRequest);

            if (response.isEmpty()) {
                return Response.accepted().build();
            } else {
                return Response.accepted(response.get()).build();
            }
        } catch (Exception e) {
            Log.errorf("Exception happened in the adapter start %s", e);
            throw new BadRequestException(e);
        }
    }

    @Override
    public Response cancel(String name, String correlationId, StopRequest stopRequest) {
        Log.infof("Cancel adapter for: '%s' with correlation-id: '%s'", name, correlationId);

        Adapter<?> adapter = adapterNameMap.get(name);

        if (adapter == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            adapter.cancel(correlationId, stopRequest);
            return Response.accepted().build();
        } catch (Exception e) {
            Log.errorf("Exception happened in the adapter cancel: %s", e);
            throw new BadRequestException(e);
        }
    }

    @Override
    public Response callback(String name, String correlationId, Object object) {
        Log.infof("Callback adapter for: '%s' with correlation-id: '%s'", name, correlationId);

        Adapter<?> adapter = adapterNameMap.get(name);

        if (adapter == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            adapter.callback(correlationId, object);
            return Response.ok().build();
        } catch (Exception e) {
            Log.errorf("Exception happened in the adapter callback: %s", e);
            throw new BadRequestException(e);
        }
    }
}
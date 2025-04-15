package org.jboss.pnc.dingrogu.application.runtime;

import kong.unirest.core.Unirest;
import kong.unirest.modules.jackson.JacksonObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.Startup;

@ApplicationScoped
public class Lifecycle {

    @Inject
    ObjectMapper objectMapper;

    @Startup
    void init() {
        // to support Optional type
        Unirest.config().setObjectMapper(new JacksonObjectMapper(objectMapper));
        Log.info("Dingrogu started");
    }

    @Shutdown
    void shutdown() {
        Log.info("Dingrogu shutdown");
    }
}

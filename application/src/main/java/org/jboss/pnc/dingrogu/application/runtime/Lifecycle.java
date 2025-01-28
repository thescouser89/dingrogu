package org.jboss.pnc.dingrogu.application.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import kong.unirest.core.Unirest;
import kong.unirest.modules.jackson.JacksonObjectMapper;

@ApplicationScoped
public class Lifecycle {

    @Startup
    void init() {
        // to support Optional type
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        Unirest.config().setObjectMapper(new JacksonObjectMapper(objectMapper));
        Log.info("Dingrogu started");
    }

    @Shutdown
    void shutdown() {
        Log.info("Dingrogu shutdown");
    }
}

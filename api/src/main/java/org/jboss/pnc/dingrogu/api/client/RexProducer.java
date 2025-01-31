package org.jboss.pnc.dingrogu.api.client;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;

import java.net.URI;

@ApplicationScoped
public class RexProducer {

    @ConfigProperty(name = "rexclient.url")
    String rexClientUrl;

    @Inject
    AuthorizationClientHttpFactory authorizationClientHttpFactory;

    @Produces
    @ApplicationScoped
    public TaskEndpoint createTaskEndpoint() {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(rexClientUrl))
                .clientHeadersFactory(authorizationClientHttpFactory)
                .build(TaskEndpoint.class);
    }

    @Produces
    @ApplicationScoped
    public CallbackEndpoint createCallbackEndpoint() {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(rexClientUrl))
                .clientHeadersFactory(authorizationClientHttpFactory)
                .build(CallbackEndpoint.class);
    }
}

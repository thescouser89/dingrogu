package org.jboss.pnc.dingrogu.api.client;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

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

    @Produces
    @ApplicationScoped
    public QueueEndpoint createQueueEndpoint() {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(rexClientUrl))
                .clientHeadersFactory(authorizationClientHttpFactory)
                .build(QueueEndpoint.class);
    }
}

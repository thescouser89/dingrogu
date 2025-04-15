package org.jboss.pnc.dingrogu.api.client;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.pnc.api.causeway.rest.Causeway;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

@ApplicationScoped
public class CausewayProducer {

    @Inject
    AuthorizationClientHttpFactory authorizationClientHttpFactory;

    public Causeway getCauseway(final String causewayUrl) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(causewayUrl))
                .clientHeadersFactory(authorizationClientHttpFactory)
                .build(Causeway.class);
    }
}

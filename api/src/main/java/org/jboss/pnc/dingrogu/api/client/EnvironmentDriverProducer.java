package org.jboss.pnc.dingrogu.api.client;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

@ApplicationScoped
public class EnvironmentDriverProducer {

    @Inject
    AuthorizationClientHttpFactory authorizationClientHttpFactory;

    public EnvironmentDriver getEnvironmentDriver(final String environmentDriverUrl) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(environmentDriverUrl))
                .clientHeadersFactory(authorizationClientHttpFactory)
                .build(EnvironmentDriver.class);
    }
}

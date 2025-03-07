package org.jboss.pnc.dingrogu.api.client;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

@ApplicationScoped
public class BuildDriverProducer {

    @Inject
    AuthorizationClientHttpFactory authorizationClientHttpFactory;

    public BuildDriver getBuildDriver(final String buildDriverUrl) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(buildDriverUrl))
                .clientHeadersFactory(authorizationClientHttpFactory)
                .build(BuildDriver.class);
    }
}

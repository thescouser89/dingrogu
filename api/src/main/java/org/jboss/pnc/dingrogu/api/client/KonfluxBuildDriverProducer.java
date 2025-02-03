package org.jboss.pnc.dingrogu.api.client;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;

@ApplicationScoped
public class KonfluxBuildDriverProducer {

    @Inject
    AuthorizationClientHttpFactory authorizationClientHttpFactory;

    public KonfluxBuildDriver getKonfluxBuildDriver(String konfluxBuildDriverUrl) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(konfluxBuildDriverUrl))
                .clientHeadersFactory(authorizationClientHttpFactory)
                .build(KonfluxBuildDriver.class);
    }
}

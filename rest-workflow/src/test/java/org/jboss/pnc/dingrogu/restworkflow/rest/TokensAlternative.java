package org.jboss.pnc.dingrogu.restworkflow.rest;

import io.quarkus.oidc.client.Tokens;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Produces;

import java.time.Duration;

@Mock
public class TokensAlternative {

    @Produces
    @RequestScoped
    public Tokens produceTokens() {
        Tokens tokens = new Tokens(
                "theToken",
                Long.MAX_VALUE,
                Duration.ofDays(1),
                "refreshToken",
                null,
                null,
                "client-id");

        return tokens;
    }
}
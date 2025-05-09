package org.jboss.pnc.dingrogu.api.client;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.pnc.common.log.MDCUtils;

import io.quarkus.oidc.client.OidcClient;

@ApplicationScoped
public class AuthorizationClientHttpFactory implements ClientHeadersFactory {

    @Inject
    OidcClient oidcClient;

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();

        // Add authorization header
        result.add(AUTHORIZATION, "Bearer " + oidcClient.getTokens().await().indefinitely().getAccessToken());

        // Add MDC headers
        Map<String, String> mdcHeaders = MDCUtils.getHeadersFromMDC();
        for (String key : mdcHeaders.keySet()) {
            System.out.println("Adding header " + key + ": " + mdcHeaders.get(key));
            result.add(key, mdcHeaders.get(key));
        }

        return result;
    }
}

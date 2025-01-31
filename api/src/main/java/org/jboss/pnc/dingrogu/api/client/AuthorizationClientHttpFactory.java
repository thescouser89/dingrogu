package org.jboss.pnc.dingrogu.api.client;

import io.quarkus.oidc.client.OidcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.pnc.common.log.MDCUtils;

import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

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
            result.add(key, mdcHeaders.get(key));
        }

        return result;
    }
}

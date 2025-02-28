package org.jboss.pnc.dingrogu.restadapter.client;

import io.quarkus.oidc.client.Tokens;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

@ApplicationScoped
public class RequestHeaderFactory implements ClientHeadersFactory {

    @Inject
    Tokens tokens;

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> multivaluedMap,
            MultivaluedMap<String, String> multivaluedMap1) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add(AUTHORIZATION, "Bearer " + tokens.getAccessToken());
        return result;
    }
}

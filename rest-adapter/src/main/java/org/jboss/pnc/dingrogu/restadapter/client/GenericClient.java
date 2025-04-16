package org.jboss.pnc.dingrogu.restadapter.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.pnc.api.dto.Request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import io.quarkus.oidc.client.Tokens;
import kong.unirest.core.ContentType;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;

@ApplicationScoped
public class GenericClient {
    @Inject
    Tokens tokens;

    @Inject
    ObjectMapper objectMapper;

    public void send(Request request) {

        try {
            Log.infof("generic client url is: %s", request.getUri().toString());
            Log.infof("generic client data is: %s", objectMapper.writeValueAsString(request.getAttachment()));
        } catch (JsonProcessingException e) {
            Log.error(e);
        }
        HttpResponse<JsonNode> response = Unirest.post(request.getUri().toString())
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(request.getAttachment())
                .asJson();

        if (!response.isSuccess()) {
            Log.errorf("Request didn't go through: HTTP %s, body: %s", response.getStatus(), response.getBody());
            throw new RuntimeException("Request didn't go through");
        }
    }
}

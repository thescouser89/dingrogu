package org.jboss.pnc.dingrogu.restadapter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.quarkus.logging.Log;
import io.quarkus.oidc.client.Tokens;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import kong.unirest.core.ContentType;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.modules.jackson.JacksonObjectMapper;
import org.jboss.pnc.api.dto.Request;

@ApplicationScoped
public class GenericClient {
    @Inject
    Tokens tokens;

    public void send(Request request) {

        // to support Optional type
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        Unirest.config().setObjectMapper(new JacksonObjectMapper(objectMapper));

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

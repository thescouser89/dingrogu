package org.jboss.pnc.dingrogu.restadapter.client;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.jboss.pnc.dingrogu.api.dto.dummy.DummyServiceRequestDTO;

@ApplicationScoped
public class DummyClient {

    public void start(String dummyUrl, String callbackUrl) {
        Log.info("Sending dummy request to server: " + dummyUrl);

        DummyServiceRequestDTO request = DummyServiceRequestDTO.builder().callbackUrl(callbackUrl).build();
        HttpResponse<JsonNode> response = Unirest.post(dummyUrl)
                .header("accept", "application/json")
                .body(request)
                .asJson();

        if (!response.isSuccess()) {
            throw new RuntimeException("Request didn't go through");
        }
    }
}

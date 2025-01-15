package org.jboss.pnc.dingrogu.restadapter.client;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.dingrogu.api.dto.dummy.DummyServiceRequestDTO;

@ApplicationScoped
public class DummyClient {

    @Retry
    public void start(String dummyUrl, String callbackUrl) {
        Log.info("Sending dummy request to server: " + dummyUrl);

        DummyServiceRequestDTO request = DummyServiceRequestDTO.builder().callbackUrl(callbackUrl).build();
        HttpResponse<JsonNode> response = Unirest.post(dummyUrl)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(request)
                .asJson();

        if (!response.isSuccess()) {
            Log.info(response.getStatus());
            Log.info(response.getStatusText());
            Log.info(response.getBody().toPrettyString());
            throw new RuntimeException("Request didn't go through");
        }
    }
}

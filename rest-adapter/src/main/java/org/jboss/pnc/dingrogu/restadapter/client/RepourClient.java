package org.jboss.pnc.dingrogu.restadapter.client;

import jakarta.enterprise.context.ApplicationScoped;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.jboss.pnc.api.repour.dto.RepourAdjustRequest;

@ApplicationScoped
public class RepourClient {

    public void adjust(String repourUrl, RepourAdjustRequest request) {

        HttpResponse<JsonNode> response = Unirest.post(repourUrl + "/adjust")
                .header("accept", "application/json")
                .body(request)
                .asJson();

        if (!response.isSuccess()) {
            throw new RuntimeException("Request didn't go through");
        }
    }
}

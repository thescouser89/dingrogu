package org.jboss.pnc.dingrogu.restadapter.client;

import jakarta.enterprise.context.ApplicationScoped;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalyzePayload;

@ApplicationScoped
public class DeliverablesAnalyzerClient {

    public void analyze(String deliverablesAnalyzerUrl, AnalyzePayload request) {

        HttpResponse<JsonNode> response = Unirest.post(deliverablesAnalyzerUrl + "/api/analyze")
                .header("accept", "application/json")
                .body(request)
                .asJson();

        if (!response.isSuccess()) {
            throw new RuntimeException("Request didn't go through");
        }
    }
}

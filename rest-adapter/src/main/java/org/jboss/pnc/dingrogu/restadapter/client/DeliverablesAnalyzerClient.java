package org.jboss.pnc.dingrogu.restadapter.client;

import io.quarkus.logging.Log;
import io.quarkus.oidc.client.Tokens;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalyzePayload;

@ApplicationScoped
public class DeliverablesAnalyzerClient {

    @Inject
    Tokens tokens;

    @Retry
    public void analyze(String deliverablesAnalyzerUrl, AnalyzePayload request) {

        HttpResponse<JsonNode> response = Unirest.post(deliverablesAnalyzerUrl + "/api/analyze")
                .header("Authorization", "Bearer " + tokens.getAccessToken())
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

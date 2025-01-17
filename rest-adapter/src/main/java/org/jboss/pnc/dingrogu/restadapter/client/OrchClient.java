package org.jboss.pnc.dingrogu.restadapter.client;

import io.quarkus.logging.Log;
import io.quarkus.oidc.client.Tokens;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import kong.unirest.core.ContentType;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalysisResult;
import org.jboss.pnc.dto.tasks.RepositoryCreationResult;

@ApplicationScoped
public class OrchClient {

    @Inject
    Tokens tokens;

    @Retry
    public void submitDelAResult(String orchUrl, AnalysisResult result) {

        Log.info("Sending dela response to server: " + orchUrl);

        HttpResponse<JsonNode> response = Unirest.post(orchUrl + "/pnc-rest/v2/deliverable-analyses/complete")
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(result)
                .asJson();

        if (!response.isSuccess()) {
            Log.info(response.getStatus());
            Log.info(response.getStatusText());
            Log.info(response.getBody().toPrettyString());
            throw new RuntimeException("Request didn't go through");
        }
    }

    @Retry
    public void submitRepourRepositoryCreationResult(String orchUrl, RepositoryCreationResult result) {

        Log.info("Sending Repour repository to server: " + orchUrl);

        // TODO: figure out endpoint
        HttpResponse<JsonNode> response = Unirest.post(orchUrl + "/pnc-rest/v2/???")
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(result)
                .asJson();

        if (!response.isSuccess()) {
            Log.info(response.getStatus());
            Log.info(response.getStatusText());
            Log.info(response.getBody().toPrettyString());
            throw new RuntimeException("Request didn't go through");
        }

    }
}

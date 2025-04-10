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
import org.jboss.pnc.api.causeway.dto.push.BuildPushCompleted;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalysisResult;
import org.jboss.pnc.api.enums.OperationResult;
import org.jboss.pnc.dto.tasks.RepositoryCreationResult;

import java.net.URI;

@ApplicationScoped
public class OrchClient {

    @Inject
    Tokens tokens;

    @Retry
    public void submitBuildPushResult(String orchUrl, String buildId, BuildPushCompleted result) {
        String orchUrlWithoutPath = URI.create(orchUrl).resolve("/").toString();

        HttpResponse<JsonNode> response = Unirest
                .post(orchUrlWithoutPath + "pnc-rest/v2/builds/" + buildId + "/brew-push/complete")
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(result)
                .asJson();

        if (!response.isSuccess()) {
            Log.errorf("Request didn't go through: HTTP %s, body: %s", response.getStatus(), response.getBody());
            throw new RuntimeException("Request didn't go through");
        }
    }

    @Retry
    public void submitDelAResult(String orchUrl, AnalysisResult result) {
        String orchUrlWithoutPath = URI.create(orchUrl).resolve("/").toString();

        HttpResponse<JsonNode> response = Unirest.post(orchUrlWithoutPath + "pnc-rest/v2/deliverable-analyses/complete")
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(result)
                .asJson();

        if (!response.isSuccess()) {
            Log.errorf("Request didn't go through: HTTP %s, body: %s", response.getStatus(), response.getBody());
            throw new RuntimeException("Request didn't go through");
        }
    }

    @Retry
    public void completeRepositoryCreation(String orchUrl, RepositoryCreationResult result) {

        Log.info("Sending Reqour repository to server: " + orchUrl);

        HttpResponse<JsonNode> response = Unirest.post(orchUrl + "/pnc-rest/v2/bpm/repository-creation/completed")
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(result)
                .asJson();

        if (!response.isSuccess()) {
            Log.errorf("Request didn't go through: HTTP %s, body: %s", response.getStatus(), response.getBody());
            throw new RuntimeException("Request didn't go through");
        }
    }

    public void completeOperation(String orchUrl, OperationResult result, String operationId) {

        String orchUrlWithoutPath = URI.create(orchUrl).resolve("/").toString();

        HttpResponse<JsonNode> response = Unirest.post(
                orchUrlWithoutPath + "pnc-rest/v2/operations/" + operationId + "/complete?result=" + result.name())
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .asJson();

        if (!response.isSuccess()) {
            Log.errorf("Request didn't go through: HTTP %s, body: %s", response.getStatus(), response.getBody());
            throw new RuntimeException("Request didn't go through");
        }
    }
}

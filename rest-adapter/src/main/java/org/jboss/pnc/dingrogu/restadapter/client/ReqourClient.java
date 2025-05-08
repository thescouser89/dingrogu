package org.jboss.pnc.dingrogu.restadapter.client;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.reqour.dto.AdjustRequest;
import org.jboss.pnc.api.reqour.dto.CancelRequest;
import org.jboss.pnc.api.reqour.dto.InternalSCMCreationRequest;
import org.jboss.pnc.api.reqour.dto.RepositoryCloneRequest;

import io.quarkus.logging.Log;
import io.quarkus.oidc.client.Tokens;
import kong.unirest.core.ContentType;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;

@ApplicationScoped
public class ReqourClient {

    @Inject
    Tokens tokens;

    @Retry
    public void adjust(String reqourUrl, AdjustRequest request) {

        HttpResponse<JsonNode> response = Unirest.post(reqourUrl + "/adjust")
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(request)
                .asJson();

        if (!response.isSuccess()) {
            processUnsuccessfulResponse(response, "/adjust");
        }
    }

    @Retry
    public void cancel(String reqourUrl, String taskId) {

        // for now we don't care about the callback
        Request callback = Request.builder().method(Request.Method.GET).uri(URI.create(reqourUrl + "/version")).build();

        CancelRequest cancelRequest = CancelRequest.builder().taskId(taskId).callback(callback).build();

        HttpResponse<JsonNode> response = Unirest.post(reqourUrl + "/cancel")
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(cancelRequest)
                .asJson();

        if (!response.isSuccess()) {
            processUnsuccessfulResponse(response, "/cancel");
        }

    }

    @Retry
    public void cloneRequest(String reqourUrl, RepositoryCloneRequest request) {

        HttpResponse<JsonNode> response = Unirest.post(reqourUrl + "/clone")
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(request)
                .asJson();

        if (!response.isSuccess()) {
            processUnsuccessfulResponse(response, "/clone");
        }
    }

    @Retry
    public void createRepository(String reqourUrl, InternalSCMCreationRequest request) {

        HttpResponse<JsonNode> response = Unirest.post(reqourUrl + "/internal-scm")
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(request)
                .asJson();

        if (!response.isSuccess()) {
            processUnsuccessfulResponse(response, "/internal-scm");
        }
    }

    private <T> void processUnsuccessfulResponse(HttpResponse<T> response, String endpoint) {
        if (response.getParsingError().isPresent()) {
            Log.errorf(
                    "Request to %s: HTTP %s, body: %s\nfinished with parsing error: %s",
                    endpoint,
                    response.getStatus(),
                    response.getBody(),
                    response.getParsingError().get());
            throw new RuntimeException("Request finished with parsing error");
        } else {
            Log.errorf(
                    "Request to %s didn't go through: HTTP %s, body: %s",
                    endpoint,
                    response.getStatus(),
                    response.getBody());
            throw new RuntimeException("Request didn't go through: " + response.getBody());
        }
    }
}

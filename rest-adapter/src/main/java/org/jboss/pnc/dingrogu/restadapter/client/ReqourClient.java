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
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.reqour.dto.AdjustRequest;
import org.jboss.pnc.api.reqour.dto.CancelRequest;
import org.jboss.pnc.api.reqour.dto.InternalSCMCreationRequest;
import org.jboss.pnc.api.reqour.dto.RepositoryCloneRequest;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCreateRepoResponse;

import java.net.URI;

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
            Log.errorf("Request didn't go through: HTTP %s, body: %s", response.getStatus(), response.getBody());
            throw new RuntimeException("Request didn't go through");
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
            Log.errorf("Cancel request didn't go through: HTTP %s, body: %s", response.getStatus(), response.getBody());
            throw new RuntimeException("Cancel request didn't go through");
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
            Log.errorf("Request didn't go through: HTTP %s, body: %s", response.getStatus(), response.getBody());
            throw new RuntimeException("Request didn't go through");
        }
    }

    @Retry
    public RepourCreateRepoResponse createRepository(String reqourUrl, InternalSCMCreationRequest request) {

        HttpResponse<RepourCreateRepoResponse> response = Unirest.post(reqourUrl + "/internal-scm")
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(request)
                .asObject(RepourCreateRepoResponse.class);

        if (!response.isSuccess()) {
            Log.errorf("Request didn't go through: HTTP %s, body: %s", response.getStatus(), response.getBody());
            throw new RuntimeException("Request didn't go through");
        }

        return response.getBody();
    }
}

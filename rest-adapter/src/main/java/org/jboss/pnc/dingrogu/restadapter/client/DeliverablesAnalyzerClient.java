package org.jboss.pnc.dingrogu.restadapter.client;

import kong.unirest.core.ContentType;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalyzePayload;
import org.jboss.pnc.dingrogu.api.dto.adapter.DelAAnalyzeResponse;

import io.quarkus.logging.Log;
import io.quarkus.oidc.client.Tokens;

@ApplicationScoped
public class DeliverablesAnalyzerClient {

    @Inject
    Tokens tokens;

    @Retry
    public DelAAnalyzeResponse analyze(String deliverablesAnalyzerUrl, AnalyzePayload request) {

        HttpResponse<DelAAnalyzeResponse> response = Unirest.post(deliverablesAnalyzerUrl + "/api/analyze")
                .contentType(ContentType.APPLICATION_JSON)
                .accept(ContentType.APPLICATION_JSON)
                .headers(ClientHelper.getClientHeaders(tokens))
                .body(request)
                .asObject(DelAAnalyzeResponse.class);

        if (!response.isSuccess()) {
            Log.errorf("Request didn't go through: HTTP %s, body: %s", response.getStatus(), response.getBody());
            throw new RuntimeException("Request didn't go through");
        }

        return response.getBody();
    }
}

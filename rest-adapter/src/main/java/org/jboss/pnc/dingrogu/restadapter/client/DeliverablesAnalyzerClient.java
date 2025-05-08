package org.jboss.pnc.dingrogu.restadapter.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalyzePayload;
import org.jboss.pnc.dingrogu.api.dto.adapter.DelAAnalyzeResponse;
import org.jboss.pnc.dingrogu.common.TaskHelper;

import io.quarkus.oidc.client.Tokens;
import kong.unirest.core.ContentType;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;

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
            TaskHelper.LIVE_LOG
                    .error("Request didn't go through: HTTP {}, body: {}", response.getStatus(), response.getBody());
            throw new RuntimeException("Request didn't go through");
        }

        return response.getBody();
    }
}

package org.jboss.pnc.dingrogu.restadapter.client;

import io.quarkus.logging.Log;
import io.quarkus.oidc.client.Tokens;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.api.constants.MDCHeaderKeys;
import org.jboss.pnc.api.repour.dto.RepourAdjustRequest;

@ApplicationScoped
public class RepourClient {

    @Inject
    Tokens tokens;

    /**
     * TODO: processContext is part of the MDC values. We're just hardcoding it to the header for now until we work on
     * the MDC values
     * 
     * @param repourUrl
     * @param request
     */
    @Retry
    public void adjust(String repourUrl, RepourAdjustRequest request) {

        HttpResponse<JsonNode> response = Unirest.post(repourUrl + "/adjust")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokens.getAccessToken())
                .header("Accept", "application/json")
                .header(MDCHeaderKeys.PROCESS_CONTEXT.getHeaderName(), request.getTaskId())
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

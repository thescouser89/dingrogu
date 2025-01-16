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
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateRequest;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateResponse;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryPromoteRequest;

@ApplicationScoped
public class RepositoryDriverClient {

    @Inject
    Tokens tokens;

    @Retry
    public RepositoryCreateResponse setup(String repositoryDriverUrl, RepositoryCreateRequest request) {
        // TODO: set all MDC values properly
        HttpResponse<RepositoryCreateResponse> response = Unirest.post(repositoryDriverUrl + "/create")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokens.getAccessToken())
                .header("Accept", "application/json")
                .header(MDCHeaderKeys.PROCESS_CONTEXT.getHeaderName(), request.getBuildContentId())
                .header(MDCHeaderKeys.TMP.getHeaderName(), Boolean.toString(request.isTempBuild()))
                .header(MDCHeaderKeys.EXP.getHeaderName(), "0")
                .header(MDCHeaderKeys.USER_ID.getHeaderName(), "dcheung")
                .body(request)
                .asObject(RepositoryCreateResponse.class);

        if (!response.isSuccess()) {
            Log.info(response.getStatus());
            Log.info(response.getStatusText());
            Log.info(response.getBody());
            throw new RuntimeException("Request didn't go through");
        }

        return response.getBody();
    }

    @Retry
    public void seal(String repositoryDriverUrl, String buildContentId) {
        // TODO: set all MDC values properly
        HttpResponse<JsonNode> response = Unirest.put(repositoryDriverUrl + "/seal")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokens.getAccessToken())
                .header("Accept", "application/json")
                .header(MDCHeaderKeys.PROCESS_CONTEXT.getHeaderName(), buildContentId)
                .header(MDCHeaderKeys.TMP.getHeaderName(), Boolean.valueOf(false).toString())
                .header(MDCHeaderKeys.EXP.getHeaderName(), "0")
                .header(MDCHeaderKeys.USER_ID.getHeaderName(), "dcheung")
                .body(buildContentId)
                .asJson();

        if (!response.isSuccess()) {
            Log.info(response.getStatus());
            Log.info(response.getStatusText());
            Log.info(response.getBody());
            throw new RuntimeException("Request didn't go through");
        }
    }

    @Retry
    public void promote(String repositoryDriverUrl, RepositoryPromoteRequest request) {
        // TODO: set all MDC values properly
        HttpResponse<JsonNode> response = Unirest.put(repositoryDriverUrl + "/promote")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokens.getAccessToken())
                .header("Accept", "application/json")
                .header(MDCHeaderKeys.PROCESS_CONTEXT.getHeaderName(), request.getBuildContentId())
                .header(MDCHeaderKeys.TMP.getHeaderName(), Boolean.valueOf(request.isTempBuild()).toString())
                .header(MDCHeaderKeys.EXP.getHeaderName(), "0")
                .header(MDCHeaderKeys.USER_ID.getHeaderName(), "dcheung")
                .body(request)
                .asJson();

        if (!response.isSuccess()) {
            Log.info(response.getStatus());
            Log.info(response.getStatusText());
            Log.info(response.getBody());
            throw new RuntimeException("Request didn't go through");
        }
    }
}

package org.jboss.pnc.dingrogu.restadapter.client;

import io.quarkus.logging.Log;
import io.quarkus.oidc.client.Tokens;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateRequest;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateResponse;

@ApplicationScoped
public class RepositoryDriverClient {

    @Inject
    Tokens tokens;

    @Retry
    public RepositoryCreateResponse setup(String repositoryDriverUrl, RepositoryCreateRequest request) {
        HttpResponse<RepositoryCreateResponse> response = Unirest.post(repositoryDriverUrl + "/create")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokens.getAccessToken())
                .header("Accept", "application/json")
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
}

package org.jboss.pnc.dingrogu.restadapter.client;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateRequest;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateResponse;

@ApplicationScoped
public class RepositoryDriverClient {

    @Retry
    public RepositoryCreateResponse setup(String repositoryDriverUrl, RepositoryCreateRequest request) {
        HttpResponse<RepositoryCreateResponse> response = Unirest.post(repositoryDriverUrl + "/create")
                .header("accept", "application/json")
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

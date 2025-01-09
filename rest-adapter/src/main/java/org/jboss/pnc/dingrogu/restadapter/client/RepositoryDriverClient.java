package org.jboss.pnc.dingrogu.restadapter.client;

import jakarta.enterprise.context.ApplicationScoped;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateRequest;

@ApplicationScoped
public class RepositoryDriverClient {

    public void setup(String repositoryDriverUrl, RepositoryCreateRequest request) {
        HttpResponse<JsonNode> response = Unirest.post(repositoryDriverUrl + "/create")
                .header("accept", "application/json")
                .body(request)
                .asJson();

        if (!response.isSuccess()) {
            throw new RuntimeException("Request didn't go through");
        }
    }
}

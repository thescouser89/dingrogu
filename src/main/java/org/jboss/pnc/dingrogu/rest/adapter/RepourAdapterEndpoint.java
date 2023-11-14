package org.jboss.pnc.dingrogu.rest.adapter;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.jboss.pnc.api.repour.dto.RepourCloneRepositoryRequest;
import org.jboss.pnc.api.repour.dto.RepourCreateRepositoryRequest;
import org.jboss.pnc.dingrogu.client.RepourClient;
import org.jboss.pnc.dingrogu.common.GitUrlParser;
import org.jboss.pnc.dingrogu.dto.RepourCloneResponse;
import org.jboss.pnc.dingrogu.dto.adapter.RepourCloneRepositoryDTO;
import org.jboss.pnc.dingrogu.dto.adapter.RepourCreateRepositoryDTO;
import org.jboss.pnc.dingrogu.dto.RepourCreateRepositoryResponse;
import org.jboss.pnc.rex.model.requests.StartRequest;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * The RepourAdapterEndpoint is used to translate Rex's StartRequest and StopRequest to Repour APIs
 */
@Path("/adapter/repour")
public class RepourAdapterEndpoint {

    /**
     * Send a request to create an internal repository for Repour. Repour doesn't require a callback for this endpoint,
     * so the response contains the result from Repour
     *
     * @param rexData request DTO from Rex
     * @return whether the request to Repour was successful or not
     */
    @Path("create-repository-start")
    @POST
    public RepourCreateRepositoryResponse createInternalRepo(StartRequest rexData) {
        RepourCreateRepositoryDTO data = (RepourCreateRepositoryDTO) rexData.getPayload();

        RepourCreateRepositoryRequest request = RepourCreateRepositoryRequest
                .builder()
                .project(GitUrlParser.generateInternalGitRepoName(data.getExternalUrl()))
                .ownerGroups(Collections.singletonList("ldap/jboss-prod"))
                .parentProject("jboss-prod-permissions")
                .build();

        // TODO: deal with MDC, authentication
        RepourClient repourClient = getRepourClient(data.getRepourUrl());
        return repourClient.createInternalRepository(request);
    }

    /**
     * Send a request to clone from an external repository to internal repository for Repour
     *
     * @param rexData request DTO from Rex
     * @return callback response
     */
    @Path("clone-repository-start")
    @POST
    public RepourCloneResponse cloneRepositoryStart(StartRequest rexData) {

        RepourCloneRepositoryDTO data = (RepourCloneRepositoryDTO) rexData.getPayload();
        Map<String, Object> previousResult = rexData.getTaskResults();

        RepourCreateRepositoryResponse response = null;

        for (Map.Entry<String, Object> item : previousResult.entrySet()) {
            if (item.getKey().contains(":internal_scm:")) {
                response = (RepourCreateRepositoryResponse) item.getValue();
                break;
            }
        }

        if (response == null) {
            throw new RuntimeException("Response not in previous tasks");
        }

        // TODO: add callback to that request
        RepourCloneRepositoryRequest request = RepourCloneRepositoryRequest
                .builder()
                .type("git")
                .originRepoUrl(data.getExternalUrl())
                .targetRepoUrl(response.getReadwriteUrl())
                .ref(data.getRef())
                .build();

        RepourClient client = getRepourClient(data.getRepourUrl());
        return client.cloneRepository(request);
    }

    private RepourClient getRepourClient(String url) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(url))
                .build(RepourClient.class);
    }
}

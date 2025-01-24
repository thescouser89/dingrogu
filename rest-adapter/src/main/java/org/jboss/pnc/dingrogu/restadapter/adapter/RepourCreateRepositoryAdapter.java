package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.api.repour.dto.RepourCreateRepositoryRequest;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCreateRepoResponse;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCreateRepositoryDTO;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.common.GitUrlParser;
import org.jboss.pnc.dingrogu.restadapter.client.RepourClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.util.Collections;

@ApplicationScoped
public class RepourCreateRepositoryAdapter implements Adapter<RepourCreateRepositoryDTO> {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    RepourClient repourClient;

    @Override
    public void start(String correlationId, StartRequest startRequest) {
        RepourCreateRepositoryDTO repourCreateDTO = objectMapper
                .convertValue(startRequest.getPayload(), RepourCreateRepositoryDTO.class);

        RepourCreateRepositoryRequest request = RepourCreateRepositoryRequest.builder()
                .project(getProjectName(repourCreateDTO.getExternalUrl()))
                .ownerGroups(Collections.singletonList("ldap/jboss-prod"))
                .parentProject("jboss-prod-permissions")
                .build();

        RepourCreateRepoResponse response = repourClient.createRepository(repourCreateDTO.getRepourUrl(), request);

        managedExecutor.submit(() -> {
            try {
                // sleep for 5 seconds to make sure that Rex has processed the successful start
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                Log.error(e);
            }
            try {
                callbackEndpoint.succeed(getRexTaskName(correlationId), response, null);
            } catch (Exception e) {
                Log.error("Error happened in rex client callback to Rex server for repository driver seal", e);
            }
        });
    }

    @Override
    public void callback(String correlationId, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.REPOSITORY_CREATION_REX_NOTIFY;
    }

    @Override
    public String getAdapterName() {
        return "repour-create-repository";
    }

    private static String getProjectName(String externalUrl) {
        return GitUrlParser.generateInternalGitRepoName(externalUrl);
    }
}

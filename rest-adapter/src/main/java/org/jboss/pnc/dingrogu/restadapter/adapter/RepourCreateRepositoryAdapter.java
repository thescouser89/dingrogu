package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.repour.dto.RepourCreateRepositoryRequest;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCreateRepoResponse;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCreateRepositoryDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.GitUrlParser;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.RepourClient;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class RepourCreateRepositoryAdapter implements Adapter<RepourCreateRepositoryDTO> {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RexClient rexClient;

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
                rexClient.invokeSuccessCallback(getRexTaskName(correlationId), response);
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
    public String getAdapterName() {
        return "repour-create-repository";
    }

    @Override
    public CreateTaskDTO generateRexTask(
            String adapterUrl,
            String correlationId,
            RepourCreateRepositoryDTO repourCreateRepositoryDTO) throws Exception {

        Request startInternalScm = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repourCreateRepositoryDTO);

        Request callerNotification = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getNotificationEndpoint(adapterUrl)),
                List.of(TaskHelper.getJsonHeader()),
                null);

        return CreateTaskDTO.builder()
                .name(getRexTaskName(correlationId))
                .remoteStart(startInternalScm)
                .configuration(new ConfigurationDTO())
                .callerNotifications(callerNotification)
                .build();
    }

    private static String getProjectName(String externalUrl) {
        return GitUrlParser.generateInternalGitRepoName(externalUrl);
    }
}

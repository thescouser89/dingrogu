package org.jboss.pnc.dingrogu.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.MilestoneReleaseRequest;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.model.Task;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class MilestoneRelease {

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    public static final String MILESTONE_RELEASE_KEY = "milestone-release:";

    CreateTaskDTO getMilestoneReleaseRequest(String milestoneId) throws Exception {
        MilestoneReleaseRequest request = MilestoneReleaseRequest.builder().milestoneId(milestoneId).build();

        Request.Header header = new Request.Header("Content-Type", "application/json");
        List<Request.Header> headers = List.of(header);
        Request startRequest = new Request(
                Request.Method.POST,
                new URI(ownUrl + "/causeway-endpoint"),
                headers,
                request);

        Request cancelRequest = new Request(
            Request.Method.POST,
            new URI("fix me later"),
            headers,
            request);

        return CreateTaskDTO
                .builder()
                .name(MILESTONE_RELEASE_KEY + milestoneId)
                .remoteStart(startRequest)
                .remoteCancel(cancelRequest)
                .configuration(new ConfigurationDTO(true, false))
                .build();
    }
    public CreateGraphRequest generateWorkflow(String milestoneId) throws Exception {

        CreateTaskDTO milestoneRelease = getMilestoneReleaseRequest(milestoneId);

        Map<String, CreateTaskDTO> vertices = Map
                .of(milestoneRelease.name, milestoneRelease);

        EdgeDTO edgeDTO = EdgeDTO.builder().source(milestoneRelease.name).target(null).build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        return new CreateGraphRequest(MILESTONE_RELEASE_KEY + "::" + milestoneId, edges, vertices);
    }
}

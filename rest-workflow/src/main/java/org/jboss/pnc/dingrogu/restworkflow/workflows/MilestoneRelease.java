package org.jboss.pnc.dingrogu.restworkflow.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.pnc.dingrogu.api.dto.MilestoneReleaseDTO;
import org.jboss.pnc.dingrogu.restworkflow.tasks.CausewayAdapterMilestoneReleaseTask;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class MilestoneRelease {

    @Inject
    CausewayAdapterMilestoneReleaseTask milestoneReleaseTask;

    public static final String MILESTONE_RELEASE_KEY = "milestone-release:";

    public CreateGraphRequest generateWorkflow(String milestoneId) throws Exception {

        MilestoneReleaseDTO milestoneReleaseDTO = MilestoneReleaseDTO.builder().milestoneId(milestoneId).build();
        CreateTaskDTO milestoneRelease = milestoneReleaseTask.getTask(milestoneReleaseDTO);

        Map<String, CreateTaskDTO> vertices = Map.of(milestoneRelease.name, milestoneRelease);

        EdgeDTO edgeDTO = EdgeDTO.builder().source(milestoneRelease.name).target(null).build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        return new CreateGraphRequest(MILESTONE_RELEASE_KEY + "::" + milestoneId, null, edges, vertices);
    }
}

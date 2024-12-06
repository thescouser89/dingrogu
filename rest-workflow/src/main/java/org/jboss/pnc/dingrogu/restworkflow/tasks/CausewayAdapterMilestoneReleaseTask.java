package org.jboss.pnc.dingrogu.restworkflow.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Setter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.MilestoneReleaseRequest;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.api.dto.MilestoneReleaseDTO;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;

import java.net.URI;
import java.util.List;

@ApplicationScoped
public class CausewayAdapterMilestoneReleaseTask implements TaskCreator<MilestoneReleaseDTO> {

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    public static final String MILESTONE_RELEASE_KEY = "milestone-release:";

    @Setter
    String milestoneId;

    @Override
    public CreateTaskDTO getTask(MilestoneReleaseDTO milestoneReleaseDTO) throws Exception {
        MilestoneReleaseRequest request = MilestoneReleaseRequest.builder()
                .milestoneId(milestoneReleaseDTO.getMilestoneId())
                .build();

        Request startRequest = new Request(
                Request.Method.POST,
                new URI(ownUrl + "/adapter/causeway"),
                List.of(TaskHelper.getJsonHeader()),
                request);

        Request cancelRequest = new Request(
                Request.Method.POST,
                new URI("fix me later"),
                List.of(TaskHelper.getJsonHeader()),
                request);

        return CreateTaskDTO.builder()
                .name(MILESTONE_RELEASE_KEY + milestoneId)
                .remoteStart(startRequest)
                .remoteCancel(cancelRequest)
                .configuration(new ConfigurationDTO())
                .build();
    }
}

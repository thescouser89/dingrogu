package org.jboss.pnc.dingrogu.restworkflow.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildWorkDTO;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.restadapter.adapter.RepositoryDriverSetupAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.RepourAdjustAdapter;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.util.Map;
import java.util.Set;

/**
 * Build process workflow implementation
 */
@ApplicationScoped
public class BuildWorkflow implements Workflow<BuildWorkDTO> {

    @Inject
    RexClient rexClient;

    @Inject
    RepourAdjustAdapter repour;

    @Inject
    RepositoryDriverSetupAdapter repoSetup;

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    @Override
    public CorrelationId submitWorkflow(BuildWorkDTO buildWorkDTO) throws WorkflowSubmissionException {
        CorrelationId correlationId = CorrelationId.generateUnique();

        try {
            CreateTaskDTO taskAlign = repour
                    .generateRexTask(ownUrl, correlationId.getId(), buildWorkDTO.toRepourAdjustDTO());
            CreateTaskDTO taskRepoSetup = repoSetup
                    .generateRexTask(ownUrl, correlationId.getId(), buildWorkDTO.toRepositoryDriverSetupDTO());

            Map<String, CreateTaskDTO> vertices = Map.of(taskAlign.name, taskAlign, taskRepoSetup.name, taskRepoSetup);

            EdgeDTO edgeDTO = EdgeDTO.builder().source(taskRepoSetup.name).target(taskAlign.name).build();
            Set<EdgeDTO> edges = Set.of(edgeDTO);

            CreateGraphRequest graphRequest = new CreateGraphRequest(correlationId.getId(), null, edges, vertices);
            rexClient.submitWorkflow(graphRequest);

            return correlationId;

        } catch (Exception e) {
            throw new WorkflowSubmissionException(e);
        }
    }
}

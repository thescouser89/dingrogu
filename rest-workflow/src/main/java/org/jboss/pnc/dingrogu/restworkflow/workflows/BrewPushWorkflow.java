package org.jboss.pnc.dingrogu.restworkflow.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.workflow.BrewPushDTO;
import org.jboss.pnc.dingrogu.restadapter.adapter.CausewayBrewPushAdapter;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of the milestone-release workflow
 */
@ApplicationScoped
public class BrewPushWorkflow implements Workflow<BrewPushDTO> {

    @Inject
    CausewayBrewPushAdapter causewayBrewPushAdapter;

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    @Override
    public CorrelationId submitWorkflow(BrewPushDTO brewPushDTO) throws WorkflowSubmissionException {

        CorrelationId uniqueCorrelationId = CorrelationId.generateUnique();

        try {
            CreateGraphRequest graph = generateWorkflow(uniqueCorrelationId, brewPushDTO);

            // TODO: send request to Rex!

            return uniqueCorrelationId;
        } catch (Exception e) {
            throw new WorkflowSubmissionException(e);
        }
    }

    CreateGraphRequest generateWorkflow(CorrelationId correlationId, BrewPushDTO brewPushDTO) throws Exception {

        CreateTaskDTO causewayBrewPush = causewayBrewPushAdapter
                .generateRexTask(ownUrl, correlationId.getId(), null, brewPushDTO);

        Map<String, CreateTaskDTO> vertices = Map.of(causewayBrewPush.name, causewayBrewPush);

        EdgeDTO edgeDTO = EdgeDTO.builder().source(causewayBrewPush.name).target(null).build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        return new CreateGraphRequest(correlationId.getId(), null, edges, vertices);
    }
}

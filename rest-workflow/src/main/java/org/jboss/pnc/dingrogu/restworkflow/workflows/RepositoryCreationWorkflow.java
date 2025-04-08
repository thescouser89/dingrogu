package org.jboss.pnc.dingrogu.restworkflow.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.adapter.ReqourCloneRepositoryDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.ReqourCreateRepositoryDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.RepositoryCreationDTO;
import org.jboss.pnc.dingrogu.restadapter.adapter.ReqourCloneRepositoryAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.ReqourCreateRepositoryAdapter;
import org.jboss.pnc.rex.api.QueueEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of the repository-creation workflow
 */
@ApplicationScoped
public class RepositoryCreationWorkflow implements Workflow<RepositoryCreationDTO> {

    @Inject
    ReqourCreateRepositoryAdapter reqourCreateRepositoryAdapter;

    @Inject
    ReqourCloneRepositoryAdapter reqourCloneRepositoryAdapter;

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    @Inject
    QueueEndpoint queueEndpoint;

    @Inject
    TaskEndpoint taskEndpoint;

    @ConfigProperty(name = "rexclient.repository_creation.queue_name")
    String rexQueueName;

    @ConfigProperty(name = "rexclient.repository_creation.queue_size")
    int rexQueueSize;

    /**
     * Submit the workflow for repository-creation to Rex, and return back the correlation id
     *
     * @param repositoryCreationDTO: workflow input
     * @return
     * @throws WorkflowSubmissionException
     */
    @Override
    public CorrelationId submitWorkflow(RepositoryCreationDTO repositoryCreationDTO)
            throws WorkflowSubmissionException {
        CorrelationId correlationId = CorrelationId.generateUnique();

        try {
            CreateGraphRequest graph = generateWorkflow(correlationId, repositoryCreationDTO);
            setRexQueueSize(queueEndpoint, rexQueueName, rexQueueSize);

            taskEndpoint.start(graph);

            return correlationId;

        } catch (Exception e) {
            throw new WorkflowSubmissionException(e);
        }
    }

    /**
     * TODO: work on the workflow
     *
     * @param correlationId
     * @param repositoryCreationDTO
     * @return
     * @throws Exception
     */
    CreateGraphRequest generateWorkflow(CorrelationId correlationId, RepositoryCreationDTO repositoryCreationDTO)
            throws Exception {
        ReqourCreateRepositoryDTO reqourCreateRepositoryDTO = ReqourCreateRepositoryDTO.builder()
                .repourUrl(repositoryCreationDTO.getReqourUrl())
                .externalUrl(repositoryCreationDTO.getExternalRepoUrl())
                .build();

        // TODO: should that be external url?
        ReqourCloneRepositoryDTO reqourCloneRepositoryDTO = ReqourCloneRepositoryDTO.builder()
                .reqourUrl(repositoryCreationDTO.getReqourUrl())
                .externalUrl(repositoryCreationDTO.getExternalRepoUrl())
                .ref(repositoryCreationDTO.getRef())
                .build();

        CreateTaskDTO taskInternalScm = reqourCreateRepositoryAdapter
                .generateRexTask(ownUrl, correlationId.getId(), repositoryCreationDTO, reqourCreateRepositoryDTO);
        CreateTaskDTO taskCloneScm = reqourCloneRepositoryAdapter
                .generateRexTask(ownUrl, correlationId.getId(), repositoryCreationDTO, reqourCloneRepositoryDTO);

        // setting up the graph
        Map<String, CreateTaskDTO> vertices = Map
                .of(taskInternalScm.name, taskInternalScm, taskCloneScm.name, taskCloneScm);

        EdgeDTO edgeDTO = EdgeDTO.builder().source(taskCloneScm.name).target(taskInternalScm.name).build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        ConfigurationDTO configurationDTO = ConfigurationDTO.builder()
                .mdcHeaderKeyMapping(org.jboss.pnc.common.log.MDCUtils.HEADER_KEY_MAPPING)
                .build();
        return new CreateGraphRequest(correlationId.getId(), rexQueueName, configurationDTO, edges, vertices);
    }
}

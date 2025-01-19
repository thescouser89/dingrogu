package org.jboss.pnc.dingrogu.restworkflow.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.workflow.RepositoryCreationDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCloneRepositoryDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCreateRepositoryDTO;
import org.jboss.pnc.dingrogu.restadapter.adapter.RepourCloneRepositoryAdapter;
import org.jboss.pnc.dingrogu.restadapter.adapter.RepourCreateRepositoryAdapter;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.util.*;

/**
 * Implementation of the repository-creation workflow
 */
@ApplicationScoped
public class RepositoryCreationWorkflow implements Workflow<RepositoryCreationDTO> {

    @Inject
    RepourCreateRepositoryAdapter repourCreateRepositoryAdapter;

    @Inject
    RepourCloneRepositoryAdapter repourCloneRepositoryAdapter;

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

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

            // TODO: submit request to rex

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
        RepourCreateRepositoryDTO repourCreateRepositoryDTO = RepourCreateRepositoryDTO.builder()
                .repourUrl(repositoryCreationDTO.getRepourUrl())
                .externalUrl(repositoryCreationDTO.getExternalRepoUrl())
                .build();

        // TODO: should that be external url?
        RepourCloneRepositoryDTO repourCloneRepositoryDTO = RepourCloneRepositoryDTO.builder()
                .repourUrl(repositoryCreationDTO.getRepourUrl())
                .externalUrl(repositoryCreationDTO.getExternalRepoUrl())
                .ref(repositoryCreationDTO.getRef())
                .build();

        CreateTaskDTO taskInternalScm = repourCreateRepositoryAdapter
                .generateRexTask(ownUrl, correlationId.getId(), repourCreateRepositoryDTO);
        CreateTaskDTO taskCloneScm = repourCloneRepositoryAdapter
                .generateRexTask(ownUrl, correlationId.getId(), repourCloneRepositoryDTO);

        // setting up the graph
        Map<String, CreateTaskDTO> vertices = Map
                .of(taskInternalScm.name, taskInternalScm, taskCloneScm.name, taskCloneScm);

        EdgeDTO edgeDTO = EdgeDTO.builder().source(taskCloneScm.name).target(taskInternalScm.name).build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        ConfigurationDTO configurationDTO = ConfigurationDTO.builder()
                .mdcHeaderKeyMapping(org.jboss.pnc.common.log.MDCUtils.HEADER_KEY_MAPPING)
                .build();
        return new CreateGraphRequest(correlationId.getId(), configurationDTO, edges, vertices);
    }
}

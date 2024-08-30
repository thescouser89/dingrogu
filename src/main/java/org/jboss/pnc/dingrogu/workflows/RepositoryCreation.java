package org.jboss.pnc.dingrogu.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.pnc.dingrogu.dto.adapter.RepourCloneRepositoryDTO;
import org.jboss.pnc.dingrogu.dto.adapter.RepourCreateRepositoryDTO;
import org.jboss.pnc.dingrogu.tasks.RepourAdapterCloneRepositoryTask;
import org.jboss.pnc.dingrogu.tasks.RepourAdapterCreateRepositoryTask;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.util.*;

@ApplicationScoped
public class RepositoryCreation {

    @Inject
    RepourAdapterCreateRepositoryTask createRepositoryTask;

    @Inject
    RepourAdapterCloneRepositoryTask cloneRepositoryTask;

    public static final String REPOSITORY_CREATION_KEY = "repository-creation:";

    public CreateGraphRequest generateWorkflow(String externalUrl, String ref) throws Exception {

        UUID uuid = UUID.randomUUID();
        RepourCreateRepositoryDTO repourCreateRepositoryDTO = RepourCreateRepositoryDTO.builder().repourUrl("1234").externalUrl(externalUrl).build();
        RepourCloneRepositoryDTO repourCloneRepositoryDTO = RepourCloneRepositoryDTO.builder().repourUrl("1234").externalUrl(externalUrl).ref(ref).build();
        CreateTaskDTO taskInternalScm = createRepositoryTask.getTask(repourCreateRepositoryDTO);
        CreateTaskDTO taskCloneScm = cloneRepositoryTask.getTask(repourCloneRepositoryDTO);

        // setting up the graph
        Map<String, CreateTaskDTO> vertices = Map
                .of(taskInternalScm.name, taskInternalScm, taskCloneScm.name, taskCloneScm);

        EdgeDTO edgeDTO = EdgeDTO.builder().source(taskCloneScm.name).target(taskInternalScm.name).build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        // TODO: deal with that null
        return new CreateGraphRequest(REPOSITORY_CREATION_KEY + "::" + uuid.toString(), null, edges, vertices);
    }
}

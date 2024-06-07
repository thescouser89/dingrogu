package org.jboss.pnc.dingrogu.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.repour.dto.RepourCloneRepositoryRequest;
import org.jboss.pnc.dingrogu.dto.adapter.RepourCreateRepositoryDTO;
import org.jboss.pnc.dingrogu.tasks.CloneRepositoryTask;
import org.jboss.pnc.dingrogu.tasks.CreateRepositoryTask;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.net.URI;
import java.util.*;

@ApplicationScoped
public class RepositoryCreation {

    @Inject
    CreateRepositoryTask createRepositoryTask;

    @Inject
    CloneRepositoryTask cloneRepositoryTask;

    public static final String REPOSITORY_CREATION_KEY = "repository-creation:";

    public CreateGraphRequest generateWorkflow(String externalUrl, String ref) throws Exception {

        UUID uuid = UUID.randomUUID();
        CreateTaskDTO taskInternalScm = createRepositoryTask.getTask();
        CreateTaskDTO taskCloneScm = cloneRepositoryTask.getTask();

        // setting up the graph
        Map<String, CreateTaskDTO> vertices = Map
                .of(taskInternalScm.name, taskInternalScm, taskCloneScm.name, taskCloneScm);

        EdgeDTO edgeDTO = EdgeDTO.builder().source(taskCloneScm.name).target(taskInternalScm.name).build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        // TODO: deal with that null
        return new CreateGraphRequest(REPOSITORY_CREATION_KEY + "::" + uuid.toString(), null, edges, vertices);
    }
}

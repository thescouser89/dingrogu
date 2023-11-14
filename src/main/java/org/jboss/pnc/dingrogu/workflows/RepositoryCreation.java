package org.jboss.pnc.dingrogu.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.repour.dto.RepourCloneRepositoryRequest;
import org.jboss.pnc.dingrogu.dto.adapter.RepourCreateRepositoryDTO;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.net.URI;
import java.util.*;

@ApplicationScoped
public class RepositoryCreation {

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    public static final String REPOSITORY_CREATION_KEY = "repository-creation:";

    public RepourCreateRepositoryDTO getRepourCreateInternalRepository(String repourUrl, String externalUrl) {

        return RepourCreateRepositoryDTO.builder()
                .repourUrl(repourUrl)
                .externalUrl(externalUrl)
                .build();
    }

    public RepourCloneRepositoryRequest getRepourCloneRepository(String externalUrl, String readWriteUrl, String ref) {

        // TODO: the way rex does callback is different. handle this
        return RepourCloneRepositoryRequest.builder()
                .type("git")
                .originRepoUrl(externalUrl)
                .targetRepoUrl(readWriteUrl)
                .ref(ref)
                // .callback(callback)
                .build();
    }

    public CreateGraphRequest generateWorkflow(String externalUrl, String ref) throws Exception {

        // TODO
        String repourUrl = "http://repour-url-placeholder";

        Request.Header header = new Request.Header("Content-Type", "application/json");
        List<Request.Header> headers = List.of(header);
        UUID uuid = UUID.randomUUID();

        Request startInternalScm = new Request(
                Request.Method.POST,
                new URI(ownUrl + "/adapter/repour/create-repository-start"),
                headers,
                getRepourCreateInternalRepository(repourUrl, externalUrl));

        CreateTaskDTO taskInternalScm = CreateTaskDTO.builder()
                .name(REPOSITORY_CREATION_KEY + ":internal-scm:" + uuid.toString())
                .remoteStart(startInternalScm)
                .configuration(new ConfigurationDTO(false, true))
                .build();

        // TODO: FIX ME HERE WTF DUSTIN
        Request startCloneScm = new Request(
                Request.Method.POST,
                new URI(ownUrl + "/adapter/repour/clone-repository-start"),
                headers,
                getRepourCloneRepository("null", "null", ref));
        // TODO: it's really /cancel/taskId
        Request cancelCloneScm = new Request(
                Request.Method.POST,
                new URI(ownUrl + "/adapter/repour/clone-repository-cancel"),
                headers);

        // TODO: need to notify PNC-Orch

        CreateTaskDTO taskCloneScm = CreateTaskDTO.builder()
                .name(REPOSITORY_CREATION_KEY + ":clone-scm:" + uuid.toString())
                .remoteStart(startCloneScm)
                .remoteCancel(cancelCloneScm)
                .build();

        // setting up the graph
        Map<String, CreateTaskDTO> vertices = Map
                .of(taskInternalScm.name, taskInternalScm, taskCloneScm.name, taskCloneScm);

        EdgeDTO edgeDTO = EdgeDTO.builder().source(taskCloneScm.name).target(taskInternalScm.name).build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        return new CreateGraphRequest(REPOSITORY_CREATION_KEY + "::" + uuid.toString(), edges, vertices);
    }
}

package org.jboss.pnc.dingrogu.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.repour.dto.RepourCloneRepositoryRequest;
import org.jboss.pnc.api.repour.dto.RepourCreateRepositoryRequest;
import org.jboss.pnc.dingrogu.common.GenerateTask;
import org.jboss.pnc.dingrogu.common.GitUrlParser;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;

import java.net.URI;
import java.util.*;

@ApplicationScoped
public class RepositoryCreation {

    public static final String REPOSITORY_CREATION_KEY = "repository-creation:";

    public RepourCreateRepositoryRequest getRepourCreateInternalRepository(String externalUrl) {

        return RepourCreateRepositoryRequest.builder()
                .project(GitUrlParser.generateInternalGitRepoName(externalUrl))
                .ownerGroups(Collections.singletonList("ldap/jboss-prod"))
                .parentProject("jboss-prod-permissions")
                .build();
    }

    public RepourCloneRepositoryRequest getRepourCloneRepository(String externalUrl, String readWriteUrl, String ref) {
        // RepourCloneCallback callback = RepourCloneCallback
        // .builder()
        // .url(systemVariables.getCallbackUrl())
        // .method(systemVariables.getCallbackMethod())
        // .build();

        // TODO: the way rex does callback is different. handle this
        return RepourCloneRepositoryRequest.builder()
                .type("git")
                .originRepoUrl(externalUrl)
                .targetRepoUrl(readWriteUrl)
                .ref(ref)
                // .callback(callback)
                .build();
    }

    public CreateGraphRequest generateWorkflow(String repourUrl, String externalUrl, String ref) throws Exception {

        Request.Header header = new Request.Header("Content-Type", "application/json");
        List<Request.Header> headers = List.of(header);
        UUID uuid = UUID.randomUUID();

        Request startInternalScm = new Request(
                Request.Method.POST,
                new URI(repourUrl + "/internal-scm"),
                headers,
                getRepourCreateInternalRepository(externalUrl));
        // TODO: it's really /cancel/taskId
        Request cancelInternalScm = new Request(Request.Method.POST, new URI(repourUrl + "/cancel"), headers);

        CreateTaskDTO taskInternalScm = CreateTaskDTO.builder()
                .name(REPOSITORY_CREATION_KEY + ":internal-scm:" + uuid.toString())
                .remoteStart(startInternalScm)
                .remoteCancel(cancelInternalScm)
                .build();

        Request startCloneScm = new Request(
                Request.Method.POST,
                new URI(repourUrl + "/clone"),
                headers,
                getRepourCloneRepository(externalUrl, "", ref));
        // TODO: it's really /cancel/taskId
        Request cancelCloneScm = new Request(Request.Method.POST, new URI(repourUrl + "/cancel"), headers);

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

package org.jboss.pnc.dingrogu.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.repour.dto.RepourCloneRepositoryRequest;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.rex.dto.CreateTaskDTO;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CloneRepositoryTask {

    public static final String REPOSITORY_CREATION_KEY = "repository-creation:";

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

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

    public CreateTaskDTO getTask() throws Exception {

        String ref = "haha";

        UUID uuid = UUID.randomUUID();

        // TODO: FIX ME HERE WTF DUSTIN
        Request startCloneScm = new Request(
                Request.Method.POST,
                new URI(ownUrl + "/adapter/repour/clone-repository-start"),
                List.of(TaskHelper.getJsonHeader()),
                getRepourCloneRepository("null", "null", ref));
        // TODO: it's really /cancel/taskId
        Request cancelCloneScm = new Request(
                Request.Method.POST,
                new URI(ownUrl + "/adapter/repour/clone-repository-cancel"),
                List.of(TaskHelper.getJsonHeader()));

        // TODO: need to notify PNC-Orch

        CreateTaskDTO taskCloneScm = CreateTaskDTO.builder()
                .name(REPOSITORY_CREATION_KEY + ":clone-scm:" + uuid)
                .remoteStart(startCloneScm)
                .remoteCancel(cancelCloneScm)
                .build();

        return taskCloneScm;
    }
}

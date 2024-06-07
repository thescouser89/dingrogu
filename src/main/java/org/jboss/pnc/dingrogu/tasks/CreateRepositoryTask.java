package org.jboss.pnc.dingrogu.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.dto.adapter.RepourCreateRepositoryDTO;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CreateRepositoryTask {

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    public static final String REPOSITORY_CREATION_KEY = "repository-creation:";

    public CreateTaskDTO getTask() throws Exception {

        // TODO
        String repourUrl = "http://repour-url-placeholder";
        String externalUrl = "hahaha";

        UUID uuid = UUID.randomUUID();

        Request startInternalScm = new Request(
                Request.Method.POST,
                new URI(ownUrl + "/adapter/repour/create-repository-start"),
                List.of(TaskHelper.getJsonHeader()),
                getRepourCreateInternalRepository(repourUrl, externalUrl));

        CreateTaskDTO taskInternalScm = CreateTaskDTO.builder()
                .name(REPOSITORY_CREATION_KEY + ":internal-scm:" + uuid)
                .remoteStart(startInternalScm)
                .configuration(new ConfigurationDTO())
                .build();

        return taskInternalScm;
    }

    public static RepourCreateRepositoryDTO getRepourCreateInternalRepository(String repourUrl, String externalUrl) {
        return RepourCreateRepositoryDTO.builder().repourUrl(repourUrl).externalUrl(externalUrl).build();
    }
}

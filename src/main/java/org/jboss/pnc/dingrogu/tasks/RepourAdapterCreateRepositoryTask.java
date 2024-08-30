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
public class RepourAdapterCreateRepositoryTask implements TaskCreator<RepourCreateRepositoryDTO> {

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    public static final String REPOSITORY_CREATION_KEY = "repository-creation:";

    @Override
    public CreateTaskDTO getTask(RepourCreateRepositoryDTO repourDTO) throws Exception {

        UUID uuid = UUID.randomUUID();

        Request startInternalScm = new Request(
                Request.Method.POST,
                new URI(ownUrl + "/adapter/repour/create-repository-start"),
                List.of(TaskHelper.getJsonHeader()),
                repourDTO);

        CreateTaskDTO taskInternalScm = CreateTaskDTO.builder()
                .name(REPOSITORY_CREATION_KEY + ":internal-scm:" + uuid)
                .remoteStart(startInternalScm)
                .configuration(new ConfigurationDTO())
                .build();

        return taskInternalScm;
    }
}

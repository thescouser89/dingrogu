package org.jboss.pnc.dingrogu.common;

import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.rest.client.RexClient;
import org.jboss.pnc.dingrogu.workflows.RepositoryCreation;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.dto.requests.FinishRequest;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class GenerateTask {

    @Inject
    RepositoryCreation repositoryCreation;

    @ConfigProperty(name = "dingrogu.url")
    String url;

    public CreateGraphRequest generateSingleRequest() throws Exception {

        Request.Header header = new Request.Header("Content-Type", "application/json");
        List<Request.Header> headers = List.of(header);
        UUID uuid = UUID.randomUUID();
        Request remoteStart = new Request(Request.Method.POST, new URI(url + "/receive-from-rex/start"), headers);
        Request remoteCancel = new Request(Request.Method.POST, new URI(url + "/receive-from-rex/cancel"), headers);

        CreateTaskDTO taskDTO = CreateTaskDTO.builder()
                .name("elvis" + uuid.toString())
                .remoteStart(remoteStart)
                .remoteCancel(remoteCancel)
                .build();

        CreateTaskDTO taskDTOCharles = CreateTaskDTO.builder()
                .name("charles" + uuid.toString())
                .remoteStart(remoteStart)
                .remoteCancel(remoteCancel)
                .build();

        Map<String, CreateTaskDTO> vertices = Map
                .of("elvis" + uuid.toString(), taskDTO, "charles" + uuid.toString(), taskDTOCharles);

        EdgeDTO edgeDTO = EdgeDTO.builder()
                .source("elvis" + uuid.toString())
                .target("charles" + uuid.toString())
                .build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        return new CreateGraphRequest("elvis", edges, vertices);
    }

    public CreateGraphRequest generateRepositoryCreation(String externalUrl) throws Exception {

        Request.Header header = new Request.Header("Content-Type", "application/json");
        List<Request.Header> headers = List.of(header);
        UUID uuid = UUID.randomUUID();

        Request remoteStart = new Request(
                Request.Method.POST,
                new URI(url + "/receive-from-rex/start"),
                headers,
                repositoryCreation.getRepourCreateInternalRepository(externalUrl));
        Request remoteCancel = new Request(Request.Method.POST, new URI(url + "/receive-from-rex/cancel"), headers);

        CreateTaskDTO taskDTO = CreateTaskDTO.builder()
                .name("elvis" + uuid.toString())
                .remoteStart(remoteStart)
                .remoteCancel(remoteCancel)
                .build();

        return null;
    }
}

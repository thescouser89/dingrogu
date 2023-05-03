package org.jboss.pnc.dingrogu;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.dto.EdgeDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.dto.requests.FinishRequest;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class GenerateTask {

    @ConfigProperty(name = "dingrogu.url")
    String url;

    public CreateGraphRequest generateSingleRequest() throws Exception {

        Request.Header header = new Request.Header("Content-Type", "application/json");
        List<Request.Header> headers = List.of(header);
        Request remoteStart = new Request(Request.Method.POST, new URI(url + "/receive-from-rex/start"), headers);
        Request remoteCancel = new Request(Request.Method.POST, new URI(url + "/receive-from-rex/cancel"), headers);

        CreateTaskDTO taskDTO = CreateTaskDTO.builder()
                .name("elvis")
                .remoteStart(remoteStart)
                .remoteCancel(remoteCancel)
                .build();

        CreateTaskDTO taskDTOCharles = CreateTaskDTO.builder()
                .name("charles")
                .remoteStart(remoteStart)
                .remoteCancel(remoteCancel)
                .build();

        Map<String, CreateTaskDTO> vertices = Map.of("elvis", taskDTO, "charles", taskDTOCharles);

        EdgeDTO edgeDTO = EdgeDTO.builder().source("elvis").target("charles").build();
        Set<EdgeDTO> edges = Set.of(edgeDTO);

        return new CreateGraphRequest("elvis", edges, vertices);
    }

    public FinishRequest callbackReply(boolean status, Object response) {
        return new FinishRequest(status, response);
    }
}

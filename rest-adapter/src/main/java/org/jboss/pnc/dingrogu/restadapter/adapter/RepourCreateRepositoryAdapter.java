package org.jboss.pnc.dingrogu.restadapter.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCreateRepositoryDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.List;

@ApplicationScoped
public class RepourCreateRepositoryAdapter implements Adapter<RepourCreateRepositoryDTO> {

    @Override
    public void start(String correlationId, StartRequest startRequest) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public void callback(String correlationId, Object object) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return "repour-create-repository";
    }

    @Override
    public CreateTaskDTO generateRexTask(
            String adapterUrl,
            String correlationId,
            RepourCreateRepositoryDTO repourCreateRepositoryDTO) throws Exception {

        Request startInternalScm = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repourCreateRepositoryDTO);

        return CreateTaskDTO.builder()
                .name(getName())
                .remoteStart(startInternalScm)
                .configuration(new ConfigurationDTO())
                .build();
    }
}

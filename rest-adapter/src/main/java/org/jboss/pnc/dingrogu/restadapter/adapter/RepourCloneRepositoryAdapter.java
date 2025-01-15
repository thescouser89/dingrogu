package org.jboss.pnc.dingrogu.restadapter.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourCloneRepositoryDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.List;

@ApplicationScoped
public class RepourCloneRepositoryAdapter implements Adapter<RepourCloneRepositoryDTO> {
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
    public String getAdapterName() {
        return "repour-clone-repository";
    }

    @Override
    public CreateTaskDTO generateRexTask(String adapterUrl, String correlationId, RepourCloneRepositoryDTO repourDTO)
            throws Exception {

        Request startCloneScm = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repourDTO);

        // TODO: it's really /cancel/taskId
        Request cancelCloneScm = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()));

        // TODO: need to notify PNC-Orch

        return CreateTaskDTO.builder()
                .name(getRexTaskName(correlationId))
                .remoteStart(startCloneScm)
                .remoteCancel(cancelCloneScm)
                .build();
    }
}

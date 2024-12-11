package org.jboss.pnc.dingrogu.restadapter.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.dto.workflow.BrewPushDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.List;

@ApplicationScoped
public class CausewayBrewPushAdapter implements Adapter<BrewPushDTO> {

    @Override
    public String getName() {
        return "causeway-brew-push";
    }

    @Override
    public void start(String correlationId, StartRequest startRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void callback(String correlationId, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateTaskDTO generateRexTask(String adapterUrl, String correlationId, BrewPushDTO brewPushDTO)
            throws Exception {

        Request startRequest = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                brewPushDTO);

        Request cancelRequest = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                brewPushDTO);

        return CreateTaskDTO.builder()
                .name(getName())
                .remoteStart(startRequest)
                .remoteCancel(cancelRequest)
                .configuration(new ConfigurationDTO())
                .build();
    }
}

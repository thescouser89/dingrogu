package org.jboss.pnc.dingrogu.restadapter.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.List;

/**
 * Just a dummy adapter to test for Rex functionality. It does nothing and just calls the Rex callback. Supports the
 * Dummy workflow
 */
@ApplicationScoped
public class DummyAdapter implements Adapter<Object> {

    @Override
    public void start(String correlationId, StartRequest startRequest) {
        Request callback = startRequest.getPositiveCallback();

        // TODO: wait a bit, then send the callback to Rex
    }

    @Override
    public void callback(String correlationId, Object object) {
        // TODO: send result to Rex via the positive/negative callback
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        // do nothing
    }

    @Override
    public String getName() {
        return "dummy-adapter";
    }

    @Override
    public CreateTaskDTO generateRexTask(String adapterUrl, String correlationId, Object object) throws Exception {

        Request dummyRequest = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                object);

        return CreateTaskDTO.builder()
                .name(getName())
                .remoteStart(dummyRequest)
                .configuration(new ConfigurationDTO())
                .build();
    }
}

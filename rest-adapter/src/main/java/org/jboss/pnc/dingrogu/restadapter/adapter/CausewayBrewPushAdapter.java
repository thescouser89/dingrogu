package org.jboss.pnc.dingrogu.restadapter.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.dingrogu.api.dto.workflow.BrewPushDTO;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.util.Optional;

@ApplicationScoped
public class CausewayBrewPushAdapter implements Adapter<BrewPushDTO> {

    @Override
    public String getAdapterName() {
        return "causeway-brew-push";
    }

    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {
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
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.BREW_PUSH_REX_NOTIFY;
    }
}

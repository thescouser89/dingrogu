package org.jboss.pnc.dingrogu.restadapter.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.dingrogu.api.dto.workflow.BrewPushDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

@ApplicationScoped
public class CausewayBrewPushAdapter implements Adapter<BrewPushDTO> {

    @Override
    public String getAdapterName() {
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
}

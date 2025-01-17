package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.adapter.DummyDTO;
import org.jboss.pnc.dingrogu.api.dto.dummy.DummyServiceResponseDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.restadapter.client.DummyClient;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

/**
 * Just a dummy adapter to test for Rex functionality. It does nothing and just calls the Rex callback. Supports the
 * Dummy workflow
 */
@ApplicationScoped
public class DummyAdapter implements Adapter<DummyDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    DummyClient dummyClient;

    @Inject
    RexClient rexClient;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void start(String correlationId, StartRequest startRequest) {
        String callbackUrl = AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getAdapterName(), correlationId);
        Log.info(startRequest.getPayload().toString());
        DummyDTO dummyDTO = objectMapper.convertValue(startRequest.getPayload(), DummyDTO.class);
        dummyClient.start(dummyDTO.getDummyServiceUrl(), callbackUrl);
    }

    @Override
    public void callback(String correlationId, Object object) {
        DummyServiceResponseDTO response = objectMapper.convertValue(object, DummyServiceResponseDTO.class);
        Log.infof("DummyService replied with: %s", response.status);
        try {
            rexClient.invokeSuccessCallback(getRexTaskName(correlationId), response);
        } catch (Exception e) {
            Log.error("Error happened in callback adapter", e);
        }
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAdapterName() {
        return "dummy-adapter";
    }
}

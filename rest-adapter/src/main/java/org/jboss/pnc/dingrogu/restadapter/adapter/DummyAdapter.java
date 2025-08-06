package org.jboss.pnc.dingrogu.restadapter.adapter;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.dingrogu.api.dto.adapter.DummyDTO;
import org.jboss.pnc.dingrogu.api.dto.dummy.DummyServiceResponseDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.restadapter.client.DummyClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import lombok.extern.slf4j.Slf4j;

/**
 * Just a dummy adapter to test for Rex functionality. It does nothing and just calls the Rex callback. Supports the
 * Dummy workflow
 */
@ApplicationScoped
@Slf4j
public class DummyAdapter implements Adapter<DummyDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    DummyClient dummyClient;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {
        Map<String, String> mdcMap = startRequest.getMdc();
        if (mdcMap != null) {
            for (String key : mdcMap.keySet()) {
                log.info("Adapter start mdc: {}::{}", key, mdcMap.get(key));
            }
        }

        String callbackUrl = AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getAdapterName(), correlationId);
        Log.info(startRequest.getPayload().toString());
        DummyDTO dummyDTO = objectMapper.convertValue(startRequest.getPayload(), DummyDTO.class);
        dummyClient.start(dummyDTO.getDummyServiceUrl(), callbackUrl);

        return Optional.empty();
    }

    @Override
    public void callback(String correlationId, Object object) {
        DummyServiceResponseDTO response = objectMapper.convertValue(object, DummyServiceResponseDTO.class);
        Log.infof("DummyService replied with: %s", response.status);
        try {
            callbackEndpoint.succeed(getRexTaskName(correlationId), response, null, null);
        } catch (Exception e) {
            Log.error("Error happened in callback adapter", e);
        }
    }

    @Override
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.DUMMY_REX_NOTIFY;
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

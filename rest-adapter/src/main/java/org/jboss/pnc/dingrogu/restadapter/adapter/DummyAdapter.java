package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.adapter.DummyDTO;
import org.jboss.pnc.dingrogu.api.dto.dummy.DummyServiceResponseDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.DummyClient;
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
@Slf4j
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
        log.info("DummyService replied with: {}", response.status);
        try {
            rexClient.invokeSuccessCallback(getRexTaskName(correlationId), response);
        } catch (Exception e) {
            log.error("Error happened in callback adapter", e);
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

    @Override
    public CreateTaskDTO generateRexTask(String adapterUrl, String correlationId, DummyDTO dummyDTO) throws Exception {

        Request dummyRequest = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                dummyDTO);

        Request cancelRequest = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                null);

        return CreateTaskDTO.builder()
                .name(getRexTaskName(correlationId))
                .remoteStart(dummyRequest)
                .remoteCancel(cancelRequest)
                .configuration(new ConfigurationDTO())
                .build();
    }
}

package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalysisReport;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalyzePayload;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.dto.adapter.DeliverablesAnalyzerDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.restadapter.client.DeliverablesAnalyzerClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DeliverablesAnalyzerAdapter implements Adapter<DeliverablesAnalyzerDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    DeliverablesAnalyzerClient deliverablesAnalyzerClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Override
    public String getAdapterName() {
        return "deliverables-analyzer";
    }

    @Override
    public Optional<Object> start(String correlationId, StartRequest startRequest) {
        DeliverablesAnalyzerDTO deliverablesAnalyzerDTO = objectMapper
                .convertValue(startRequest.getPayload(), DeliverablesAnalyzerDTO.class);

        String callbackUrl = AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getAdapterName(), correlationId);
        Request callback = new Request(Request.Method.POST, URI.create(callbackUrl), List.of());

        // TODO: heartbeat
        AnalyzePayload payload = new AnalyzePayload(
                deliverablesAnalyzerDTO.getOperationId(),
                deliverablesAnalyzerDTO.getUrls(),
                deliverablesAnalyzerDTO.getConfig(),
                callback,
                null);

        deliverablesAnalyzerClient.analyze(deliverablesAnalyzerDTO.getDeliverablesAnalyzerUrl(), payload);
        return Optional.empty();
    }

    @Override
    public void callback(String correlationId, Object object) {
        AnalysisReport report = objectMapper.convertValue(object, AnalysisReport.class);
        try {
            callbackEndpoint.succeed(getRexTaskName(correlationId), report, null);
        } catch (Exception e) {
            Log.error("Error happened in callback adapter", e);
        }
    }

    @Override
    public String getNotificationEndpoint(String adapterUrl) {
        return adapterUrl + WorkflowEndpoint.DELIVERABLES_ANALYSIS_REX_NOTIFY;
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }
}
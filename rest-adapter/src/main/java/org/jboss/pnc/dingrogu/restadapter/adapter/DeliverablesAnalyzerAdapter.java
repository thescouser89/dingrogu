package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalysisReport;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalyzePayload;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.client.RexClient;
import org.jboss.pnc.dingrogu.api.dto.adapter.DeliverablesAnalyzerDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.DeliverablesAnalyzerClient;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.List;

@ApplicationScoped
public class DeliverablesAnalyzerAdapter implements Adapter<DeliverablesAnalyzerDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    DeliverablesAnalyzerClient deliverablesAnalyzerClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RexClient rexClient;

    @Override
    public String getAdapterName() {
        return "deliverables-analyzer";
    }

    @Override
    public void start(String correlationId, StartRequest startRequest) {
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
    }

    @Override
    public void callback(String correlationId, Object object) {
        AnalysisReport report = objectMapper.convertValue(object, AnalysisReport.class);
        try {
            rexClient.invokeSuccessCallback(getRexTaskName(correlationId), report);
        } catch (Exception e) {
            Log.error("Error happened in callback adapter", e);
        }
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateTaskDTO generateRexTask(
            String adapterUrl,
            String correlationId,
            DeliverablesAnalyzerDTO deliverablesAnalyzerDTO) throws Exception {
        Request startRequest = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                deliverablesAnalyzerDTO);

        Request cancelRequest = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                deliverablesAnalyzerDTO);

        Request callerNotification = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getNotificationEndpoint(adapterUrl)),
                List.of(TaskHelper.getJsonHeader()),
                null);

        return CreateTaskDTO.builder()
                .name(getRexTaskName(correlationId))
                .remoteStart(startRequest)
                .remoteCancel(cancelRequest)
                .configuration(new ConfigurationDTO())
                .callerNotifications(callerNotification)
                .build();
    }
}
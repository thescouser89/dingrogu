package org.jboss.pnc.dingrogu.restadapter.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalysisReport;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalysisResult;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.dto.adapter.OrchDeliverablesAnalyzerResultDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.OrchClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.ServerResponse;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@ApplicationScoped
public class OrchDeliverablesAnalyzerResultAdapter implements Adapter<OrchDeliverablesAnalyzerResultDTO> {

    @ConfigProperty(name = "dingrogu.url")
    String dingroguUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    OrchClient orchClient;

    @Inject
    DeliverablesAnalyzerAdapter deliverablesAnalyzerAdapter;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Override
    public String getAdapterName() {
        return "orch-dela-result";
    }

    @Override
    public void start(String correlationId, StartRequest startRequest) {
        Request callback;
        try {
            callback = new Request(
                    Request.Method.POST,
                    new URI(AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getAdapterName(), correlationId)),
                    TaskHelper.getHTTPHeaders(),
                    null);
        } catch (URISyntaxException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }

        // grab payload DTO
        OrchDeliverablesAnalyzerResultDTO dto = objectMapper
                .convertValue(startRequest.getPayload(), OrchDeliverablesAnalyzerResultDTO.class);

        Map<String, Object> pastResults = startRequest.getTaskResults();
        Object pastResult = pastResults.get(deliverablesAnalyzerAdapter.getRexTaskName(correlationId));
        ServerResponse serverResponse = objectMapper.convertValue(pastResult, ServerResponse.class);
        AnalysisReport analysisReport = objectMapper.convertValue(serverResponse.getBody(), AnalysisReport.class);

        // generate result for Orch
        AnalysisResult result = AnalysisResult.builder()
                .operationId(dto.getOperationId())
                .results(analysisReport.getResults())
                .scratch(dto.isScratch())
                .callback(callback)
                .build();

        orchClient.submitDelAResult(dto.getOrchUrl(), result);
    }

    @Override
    public void callback(String correlationId, Object object) {
        try {
            callbackEndpoint.succeed(getRexTaskName(correlationId), null, null);
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

    /**
     * We read past results to build final request
     * 
     * @return true
     */
    @Override
    public boolean shouldGetResultsFromDependencies() {
        return true;
    }
}

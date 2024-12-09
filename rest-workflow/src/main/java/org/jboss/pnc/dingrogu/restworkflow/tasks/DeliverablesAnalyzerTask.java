package org.jboss.pnc.dingrogu.restworkflow.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalyzePayload;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.Endpoints;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.rex.dto.CreateTaskDTO;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Class to generate the Rex task from input. The Rex task will send requests to the Rex adapter, and the latter will
 * send the request to the PNC service
 */
@ApplicationScoped
public class DeliverablesAnalyzerTask {

    public static final String DELIVERABLES_ANALYZER_KEY = "deliverables-analyzer:";

    @ConfigProperty(name = "dingrogu.url")
    public String ownUrl;

    public CreateTaskDTO getTask(AnalyzePayload analyzePayload, String rexCorrelationId) throws Exception {

        UUID uuid = UUID.randomUUID();

        Request startAnalyze = new Request(
                Request.Method.POST,
                new URI(ownUrl + Endpoints.ADAPTER_DELIVERABLES_ANALYZER_ANALYZE_START),
                List.of(TaskHelper.getJsonHeader()),
                analyzePayload);

        Request cancelAnalyze = new Request(
                Request.Method.POST,
                new URI(ownUrl + Endpoints.ADAPTER_DELIVERABLES_ANALYZER_ANALYZE_CANCEL),
                List.of(TaskHelper.getJsonHeader()));

        return CreateTaskDTO.builder()
                .name(rexCorrelationId + "::" + DELIVERABLES_ANALYZER_KEY + ":analyze:")
                .remoteStart(startAnalyze)
                .remoteCancel(cancelAnalyze)
                .build();
    }
}

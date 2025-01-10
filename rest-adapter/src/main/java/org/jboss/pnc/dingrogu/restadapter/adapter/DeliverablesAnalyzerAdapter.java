package org.jboss.pnc.dingrogu.restadapter.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalyzePayload;
import org.jboss.pnc.api.dto.Request;
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

    @Override
    public String getName() {
        return "deliverables-analyzer";
    }

    @Override
    public void start(String correlationId, StartRequest startRequest) {
        DeliverablesAnalyzerDTO deliverablesAnalyzerDTO = (DeliverablesAnalyzerDTO) startRequest.getPayload();

        String callbackUrl = AdapterEndpoint.getCallbackAdapterEndpoint(dingroguUrl, getName(), correlationId);
        Request callback = new Request(Request.Method.POST, URI.create(callbackUrl), null);

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
        // TODO: send reply back to rex

    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateTaskDTO generateRexTask(
            String adapterUrl,
            String correlationId,
            DeliverablesAnalyzerDTO deliverablesAnalyzerDTO) throws Exception {
        Request startRequest = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                deliverablesAnalyzerDTO);

        Request cancelRequest = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                deliverablesAnalyzerDTO);

        return CreateTaskDTO.builder()
                .name(getName())
                .remoteStart(startRequest)
                .remoteCancel(cancelRequest)
                .configuration(new ConfigurationDTO())
                .build();
    }
}
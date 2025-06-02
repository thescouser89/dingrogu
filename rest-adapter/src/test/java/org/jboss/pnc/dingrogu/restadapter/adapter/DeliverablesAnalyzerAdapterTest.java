package org.jboss.pnc.dingrogu.restadapter.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;

import jakarta.inject.Inject;

import org.instancio.Instancio;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalysisReport;
import org.jboss.pnc.api.deliverablesanalyzer.dto.AnalyzePayload;
import org.jboss.pnc.api.deliverablesanalyzer.dto.FinderResult;
import org.jboss.pnc.api.dto.HeartbeatConfig;
import org.jboss.pnc.dingrogu.api.dto.adapter.DeliverablesAnalyzerDTO;
import org.jboss.pnc.dingrogu.restadapter.client.DeliverablesAnalyzerClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class DeliverablesAnalyzerAdapterTest {

    @InjectMock
    CallbackEndpoint callbackEndpoint;

    @InjectMock
    DeliverablesAnalyzerClient client;

    @Inject
    DeliverablesAnalyzerAdapter deliverablesAnalyzerAdapter;

    @BeforeEach
    void setupMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getAdapterName() {
        assertThat(deliverablesAnalyzerAdapter.getAdapterName()).isNotEmpty();
        assertThat(deliverablesAnalyzerAdapter.getAdapterName()).contains("deliverables-analyzer");
    }

    @Test
    void start() {
        // given random data
        String correlationId = "testmecorrelation1234";

        // generate random DTO
        DeliverablesAnalyzerDTO dto = Instancio.create(DeliverablesAnalyzerDTO.class);
        HeartbeatConfig heartbeatConfig = Instancio.create(HeartbeatConfig.class);
        StartRequest startRequest = StartRequest.builder().payload(dto).heartbeatConfig(heartbeatConfig).build();

        Mockito.when(client.analyze(any(), any())).thenReturn(null);

        // send request
        deliverablesAnalyzerAdapter.start(correlationId, startRequest);

        // capture the parameters sent to Causeway
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AnalyzePayload> captor = ArgumentCaptor.forClass(AnalyzePayload.class);
        Mockito.verify(client).analyze(urlCaptor.capture(), captor.capture());

        AnalyzePayload payload = captor.getValue();

        assertThat(urlCaptor.getValue()).isEqualTo(dto.getDeliverablesAnalyzerUrl());
        assertThat(payload.getCallback()).isNotNull();
        assertThat(payload.getUrls()).isEqualTo(dto.getUrls());
        assertThat(payload.getOperationId()).isEqualTo(dto.getOperationId());
        assertThat(payload.getHeartbeat()).isEqualTo(heartbeatConfig);
    }

    @Test
    void successCallback() {
        // given a success response from dela
        AnalysisReport report = AnalysisReport.builder()
                .success(true)
                .results(List.of(FinderResult.builder().build()))
                .build();

        String correlationId = "correlation-12345";
        deliverablesAnalyzerAdapter.callback(correlationId, report);
        Mockito.verify(callbackEndpoint)
                .succeed(deliverablesAnalyzerAdapter.getRexTaskName(correlationId), report, null);
    }

    @Test
    void failCallback() {

        // given a bad response from causeway
        AnalysisReport report = AnalysisReport.builder()
                .success(false)
                .results(Instancio.createList(FinderResult.class))
                .build();

        String correlationId = "correlation-12345";
        deliverablesAnalyzerAdapter.callback(correlationId, report);
        // verify that the successful callback is called
        Mockito.verify(callbackEndpoint).fail(deliverablesAnalyzerAdapter.getRexTaskName(correlationId), report, null);
    }

    @Test
    void noResponseCallback() {

        // given a no response from causeway
        AnalysisReport report = null;

        String correlationId = "correlation-12345";
        deliverablesAnalyzerAdapter.callback(correlationId, report);
        // verify that the successful callback is called
        Mockito.verify(callbackEndpoint).fail(deliverablesAnalyzerAdapter.getRexTaskName(correlationId), report, null);
    }

    @Test
    void notParseableCallback() {

        // given a bad DTO response from causeway that cannot be parsed to PushResult
        String report = "test-me-i-shoul-fail";

        String correlationId = "correlationid-1234";
        deliverablesAnalyzerAdapter.callback(correlationId, report);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint).fail(deliverablesAnalyzerAdapter.getRexTaskName(correlationId), report, null);
    }

}
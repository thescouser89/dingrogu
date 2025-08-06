package org.jboss.pnc.dingrogu.restadapter.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.jboss.pnc.rex.common.enums.ResponseFlag.SKIP_ROLLBACK;
import static org.mockito.ArgumentMatchers.any;

import java.util.Set;

import jakarta.inject.Inject;

import org.instancio.Instancio;
import org.jboss.pnc.api.causeway.dto.push.BuildPushRequest;
import org.jboss.pnc.api.causeway.dto.push.PushResult;
import org.jboss.pnc.api.causeway.rest.Causeway;
import org.jboss.pnc.api.dto.HeartbeatConfig;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.dingrogu.api.client.CausewayProducer;
import org.jboss.pnc.dingrogu.api.dto.adapter.BrewPushDTO;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class CausewayBuildPushAdapterTest {

    @InjectMock
    CausewayProducer causewayProducer;

    @Inject
    CausewayBuildPushAdapter causewayBuildPushAdapter;

    @InjectMock
    CallbackEndpoint callbackEndpoint;

    @Mock
    Causeway causeway;

    @BeforeEach
    void setupMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getAdapterName() {
        assertThat(causewayBuildPushAdapter.getAdapterName()).isNotEmpty();
        assertThat(causewayBuildPushAdapter.getAdapterName()).contains("causeway");
    }

    @Test
    void start() {
        // given random data
        String correlationId = "testmecorrelation1234";

        // generate random DTO
        BrewPushDTO dto = Instancio.create(BrewPushDTO.class);
        HeartbeatConfig heartbeatConfig = Instancio.create(HeartbeatConfig.class);
        StartRequest startRequest = StartRequest.builder().payload(dto).heartbeatConfig(heartbeatConfig).build();

        assertThat(causeway).isNotNull();
        Mockito.when(causewayProducer.getCauseway(any())).thenReturn(causeway);

        // send request
        causewayBuildPushAdapter.start(correlationId, startRequest);

        // capture the parameters sent to Causeway
        ArgumentCaptor<BuildPushRequest> captor = ArgumentCaptor.forClass(BuildPushRequest.class);
        Mockito.verify(causeway).importBuild(captor.capture());
        BuildPushRequest generated = captor.getValue();
        assertThat(generated.getBuildId()).isEqualTo(dto.getBuildId());
        assertThat(generated.getTagPrefix()).isEqualTo(dto.getTagPrefix());
        assertThat(generated.getUsername()).isEqualTo(dto.getUsername());
        assertThat(generated.getCallback()).isNotNull();
        assertThat(generated.getHeartbeat()).isEqualTo(heartbeatConfig);
    }

    @Test
    void successCallback() {
        // given a success response from causeway
        PushResult pushResult = PushResult.builder()
                .buildId("1234")
                .brewBuildId(5)
                .result(ResultStatus.SUCCESS)
                .brewBuildUrl("brew build url")
                .build();

        String correlationId = "correlation-12345";
        causewayBuildPushAdapter.callback(correlationId, pushResult);
        // verify that the successful callback is called
        Mockito.verify(callbackEndpoint)
                .succeed(causewayBuildPushAdapter.getRexTaskName(correlationId), pushResult, null, null);
    }

    @Test
    void failCallback() {

        // given a bad response from causeway
        PushResult pushResult = PushResult.builder()
                .buildId("1234")
                .brewBuildId(5)
                .result(ResultStatus.FAILED)
                .brewBuildUrl("brew build url")
                .build();

        String correlationId = "correlationid-1234";
        causewayBuildPushAdapter.callback(correlationId, pushResult);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(causewayBuildPushAdapter.getRexTaskName(correlationId), pushResult, null, Set.of(SKIP_ROLLBACK));
    }

    @Test
    void systemErrorCallback() {

        // given a bad response from causeway
        PushResult pushResult = PushResult.builder()
                .buildId("1234")
                .brewBuildId(5)
                .result(ResultStatus.SYSTEM_ERROR)
                .brewBuildUrl("brew build url")
                .build();

        String correlationId = "correlationid-1234";
        causewayBuildPushAdapter.callback(correlationId, pushResult);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(causewayBuildPushAdapter.getRexTaskName(correlationId), pushResult, null, null);
    }

    @Test
    void noResponseCallback() {

        // given a no response from causeway
        PushResult pushResult = null;

        String correlationId = "correlationid-1234";
        causewayBuildPushAdapter.callback(correlationId, pushResult);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(causewayBuildPushAdapter.getRexTaskName(correlationId), pushResult, null, null);
    }

    @Test
    void notParseableCallback() {

        // given a bad DTO response from causeway that cannot be parsed to PushResult
        String pushResult = "test-me-i-shoul-fail";

        String correlationId = "correlationid-1234";
        causewayBuildPushAdapter.callback(correlationId, pushResult);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(causewayBuildPushAdapter.getRexTaskName(correlationId), pushResult, null, null);
    }
}
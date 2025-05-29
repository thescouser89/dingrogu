package org.jboss.pnc.dingrogu.restadapter.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.instancio.Instancio;
import org.jboss.pnc.api.builddriver.dto.BuildCancelRequest;
import org.jboss.pnc.api.builddriver.dto.BuildCompleted;
import org.jboss.pnc.api.builddriver.dto.BuildRequest;
import org.jboss.pnc.api.builddriver.dto.BuildResponse;
import org.jboss.pnc.api.dto.HeartbeatConfig;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateResult;
import org.jboss.pnc.api.reqour.dto.AdjustResponse;
import org.jboss.pnc.dingrogu.api.client.BuildDriver;
import org.jboss.pnc.dingrogu.api.client.BuildDriverProducer;
import org.jboss.pnc.dingrogu.api.dto.adapter.BuildDriverDTO;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.dto.ServerResponseDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BuildDriverAdapterTest {

    @Inject
    BuildDriverAdapter buildDriverAdapter;

    @Inject
    EnvironmentDriverCreateAdapter environmentDriverCreateAdapter;

    @Inject
    ReqourAdjustAdapter reqourAdjustAdapter;

    @InjectMock
    BuildDriverProducer buildDriverProducer;

    @Mock
    BuildDriver buildDriver;

    @InjectMock
    CallbackEndpoint callbackEndpoint;

    @InjectMock
    TaskEndpoint taskEndpoint;

    @BeforeEach
    void setupMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getAdapterName() {
        assertThat(buildDriverAdapter.getAdapterName()).isNotEmpty();
        assertThat(buildDriverAdapter.getAdapterName()).contains("build");
        assertThat(buildDriverAdapter.getAdapterName()).contains("driver");
    }

    @Test
    void start() {
        // generate random DTO
        String correlationId = "123";
        BuildDriverDTO dto = Instancio.create(BuildDriverDTO.class);

        // generate environment create result
        Map<String, Object> pastResults = new HashMap<>();
        EnvironmentCreateResult environmentCreateResult = Instancio.create(EnvironmentCreateResult.class);
        pastResults.put(environmentDriverCreateAdapter.getRexTaskName(correlationId), environmentCreateResult);

        // generate adjust response
        AdjustResponse adjustResponse = Instancio.create(AdjustResponse.class);
        pastResults.put(reqourAdjustAdapter.getRexTaskName(correlationId), adjustResponse);

        // when build driver gets request, return a response
        CompletionStage<BuildResponse> buildResponse = CompletableFuture
                .supplyAsync(() -> Instancio.create(BuildResponse.class));
        Mockito.when(buildDriver.build(any())).thenReturn(buildResponse);
        Mockito.when(buildDriverProducer.getBuildDriver(any())).thenReturn(buildDriver);

        // generate start request
        HeartbeatConfig heartbeatConfig = Instancio.create(HeartbeatConfig.class);
        StartRequest startRequest = StartRequest.builder()
                .payload(dto)
                .heartbeatConfig(heartbeatConfig)
                .taskResults(pastResults)
                .build();

        // send start request
        buildDriverAdapter.start(correlationId, startRequest);

        // capture the parameters sent to build driver
        ArgumentCaptor<BuildRequest> captor = ArgumentCaptor.forClass(BuildRequest.class);
        Mockito.verify(buildDriver).build(captor.capture());
        BuildRequest generated = captor.getValue();

        // verify that the build request sent to build driver is generated properly
        assertThat(generated.getCommand()).isEqualTo(dto.getBuildCommand());
        assertThat(generated.isDebugEnabled()).isEqualTo(dto.isDebugEnabled());
        assertThat(generated.getScmUrl()).isEqualTo(adjustResponse.getInternalUrl().getReadonlyUrl());
        assertThat(generated.getScmRevision()).isEqualTo(adjustResponse.getDownstreamCommit());
        assertThat(generated.getScmTag()).isEqualTo(adjustResponse.getTag());
        assertThat(generated.getWorkingDirectory()).isEqualTo(environmentCreateResult.getWorkingDirectory());
        assertThat(generated.getEnvironmentBaseUrl())
                .isEqualTo(environmentCreateResult.getEnvironmentBaseUri().toString());
        assertThat(generated.getHeartbeatConfig()).isEqualTo(heartbeatConfig);
    }

    @Test
    void successCallback() {
        // given a success response from build driver
        BuildCompleted response = BuildCompleted.builder().buildStatus(ResultStatus.SUCCESS).build();

        String correlationId = "123";
        buildDriverAdapter.callback(correlationId, response);

        // verify that the successful callback is called
        Mockito.verify(callbackEndpoint).succeed(buildDriverAdapter.getRexTaskName(correlationId), response, null);
    }

    @Test
    void failCallback() {
        // given a bad response from build driver
        BuildCompleted response = BuildCompleted.builder().buildStatus(ResultStatus.FAILED).build();

        String correlationId = "123";
        buildDriverAdapter.callback(correlationId, response);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint).fail(buildDriverAdapter.getRexTaskName(correlationId), response, null);
    }

    @Test
    void noResponseCallback() {
        // given a null response from build driver
        BuildCompleted response = null;

        String correlationId = "123";
        buildDriverAdapter.callback(correlationId, response);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint).fail(buildDriverAdapter.getRexTaskName(correlationId), response, null);
    }

    @Test
    void notParseableCallback() {
        // given a bad DTO response from build driver that cannot be parsed to BuildCompleted
        String response = "foo";

        String correlationId = "123";
        buildDriverAdapter.callback(correlationId, response);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint).fail(buildDriverAdapter.getRexTaskName(correlationId), response, null);
    }

    @Test
    void cancel() {
        // generate random DTO
        String correlationId = "123";
        BuildDriverDTO dto = Instancio.create(BuildDriverDTO.class);

        // generate environment create result
        Map<String, Object> pastResults = new HashMap<>();
        EnvironmentCreateResult environmentCreateResult = Instancio.create(EnvironmentCreateResult.class);
        pastResults.put(environmentDriverCreateAdapter.getRexTaskName(correlationId), environmentCreateResult);

        // when task endpoint gets request, return a response
        BuildResponse buildResponse = BuildResponse.builder().buildExecutionId("foo").build();
        ServerResponseDTO serverResponse = ServerResponseDTO.builder().body(buildResponse).build();
        TaskDTO task = TaskDTO.builder().serverResponses(Collections.singletonList(serverResponse)).build();
        Mockito.when(taskEndpoint.getSpecific(buildDriverAdapter.getRexTaskName(correlationId))).thenReturn(task);

        // when build driver gets request, return a response
        CompletionStage<Response> response = CompletableFuture.supplyAsync(() -> Instancio.create(Response.class));
        Mockito.when(buildDriver.cancel(any())).thenReturn(response);
        Mockito.when(buildDriverProducer.getBuildDriver(any())).thenReturn(buildDriver);

        // generate stop request
        StopRequest stopRequest = StopRequest.builder().payload(dto).taskResults(pastResults).build();

        // send stop request
        buildDriverAdapter.cancel(correlationId, stopRequest);

        // capture the parameters sent to environment driver
        ArgumentCaptor<BuildCancelRequest> captor = ArgumentCaptor.forClass(BuildCancelRequest.class);
        Mockito.verify(buildDriver).cancel(captor.capture());
        BuildCancelRequest generated = captor.getValue();

        // verify that the build cancel request sent to build driver is generated properly
        assertThat(generated.getBuildEnvironmentBaseUrl())
                .isEqualTo(environmentCreateResult.getEnvironmentBaseUri().toString());
        assertThat(generated.getBuildExecutionId()).isEqualTo(buildResponse.getBuildExecutionId());
    }
}
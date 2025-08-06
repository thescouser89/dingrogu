package org.jboss.pnc.dingrogu.restadapter.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;

import org.instancio.Instancio;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCompleteResponse;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateRequest;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateResponse;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateResult;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateResponse;
import org.jboss.pnc.dingrogu.api.client.EnvironmentDriver;
import org.jboss.pnc.dingrogu.api.client.EnvironmentDriverProducer;
import org.jboss.pnc.dingrogu.api.dto.adapter.EnvironmentDriverCreateDTO;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.State;
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
class EnvironmentDriverCreateAdapterTest {

    @Inject
    EnvironmentDriverCreateAdapter environmentDriverCreateAdapter;

    @Inject
    RepositoryDriverSetupAdapter repositoryDriverSetupAdapter;

    @InjectMock
    EnvironmentDriverProducer environmentDriverProducer;

    @Mock
    EnvironmentDriver environmentDriver;

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
        assertThat(environmentDriverCreateAdapter.getAdapterName()).isNotEmpty();
        assertThat(environmentDriverCreateAdapter.getAdapterName()).contains("environment");
        assertThat(environmentDriverCreateAdapter.getAdapterName()).contains("driver");
        assertThat(environmentDriverCreateAdapter.getAdapterName()).contains("create");
    }

    @Test
    void start() {
        // generate random DTO
        String correlationId = "123";
        EnvironmentDriverCreateDTO dto = Instancio.create(EnvironmentDriverCreateDTO.class);

        // generate repository response
        Map<String, Object> pastResults = new HashMap<>();
        RepositoryCreateResponse repositoryResponse = Instancio.create(RepositoryCreateResponse.class);
        pastResults.put(repositoryDriverSetupAdapter.getRexTaskName(correlationId), repositoryResponse);

        // when environment driver gets request, return a response
        CompletionStage<EnvironmentCreateResponse> environmentCreateResponse = CompletableFuture
                .supplyAsync(() -> Instancio.create(EnvironmentCreateResponse.class));
        Mockito.when(environmentDriver.build(any())).thenReturn(environmentCreateResponse);
        Mockito.when(environmentDriverProducer.getEnvironmentDriver(any())).thenReturn(environmentDriver);

        // generate start request
        StartRequest startRequest = StartRequest.builder().payload(dto).taskResults(pastResults).build();

        // send start request
        environmentDriverCreateAdapter.start(correlationId, startRequest);

        // capture the parameters sent to environment driver
        ArgumentCaptor<EnvironmentCreateRequest> captor = ArgumentCaptor.forClass(EnvironmentCreateRequest.class);
        Mockito.verify(environmentDriver).build(captor.capture());
        EnvironmentCreateRequest generated = captor.getValue();

        // verify that the environment create request sent to environment driver is generated properly
        assertThat(generated.getEnvironmentLabel()).isEqualTo(dto.getEnvironmentLabel().toLowerCase(Locale.ROOT));
        assertThat(generated.getImageId()).isEqualTo(dto.getEnvironmentImage());
        assertThat(generated.getRepositoryBuildContentId()).isEqualTo(dto.getBuildContentId());
        assertThat(generated.getPodMemoryOverride()).isEqualTo(dto.getPodMemoryOverride());
        assertThat(generated.isAllowSshDebug()).isEqualTo(dto.isDebugEnabled());
        assertThat(generated.getBuildConfigId()).isEqualTo(dto.getBuildConfigId());
        assertThat(generated.getRepositoryDependencyUrl()).isEqualTo(repositoryResponse.getRepositoryDependencyUrl());
        assertThat(generated.getRepositoryDeployUrl()).isEqualTo(repositoryResponse.getRepositoryDeployUrl());
        assertThat(generated.isSidecarEnabled()).isEqualTo(repositoryResponse.isSidecarEnabled());
        assertThat(generated.isSidecarArchiveEnabled()).isEqualTo(repositoryResponse.isSidecarArchiveEnabled());
    }

    @Test
    void successCallback() {
        // given a success response from environment driver
        EnvironmentCreateResult response = EnvironmentCreateResult.builder().status(ResultStatus.SUCCESS).build();

        String correlationId = "123";
        environmentDriverCreateAdapter.callback(correlationId, response);

        // verify that the successful callback is called
        Mockito.verify(callbackEndpoint)
                .succeed(environmentDriverCreateAdapter.getRexTaskName(correlationId), response, null, null);
    }

    @Test
    void failCallback() {
        // given a bad response from environment driver
        EnvironmentCreateResult response = EnvironmentCreateResult.builder().status(ResultStatus.FAILED).build();

        String correlationId = "123";
        environmentDriverCreateAdapter.callback(correlationId, response);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(environmentDriverCreateAdapter.getRexTaskName(correlationId), response, null, null);
    }

    @Test
    void systemErrorCallback() {
        // given a bad response from environment driver
        EnvironmentCreateResult response = EnvironmentCreateResult.builder().status(ResultStatus.SYSTEM_ERROR).build();

        String correlationId = "123";
        environmentDriverCreateAdapter.callback(correlationId, response);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(environmentDriverCreateAdapter.getRexTaskName(correlationId), response, null, null);
    }

    @Test
    void noResponseCallback() {
        // given a null response from environment driver
        EnvironmentCreateResult response = null;

        String correlationId = "123";
        environmentDriverCreateAdapter.callback(correlationId, response);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(environmentDriverCreateAdapter.getRexTaskName(correlationId), response, null, null);
    }

    @Test
    void notParseableCallback() {

        // given a bad DTO response from environment driver that cannot be parsed to EnvironmentCreateResult
        String response = "foo";

        String correlationId = "123";
        environmentDriverCreateAdapter.callback(correlationId, response);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(environmentDriverCreateAdapter.getRexTaskName(correlationId), response, null, null);
    }

    @Test
    void cancel() {
        // generate random DTO
        String correlationId = "123";
        EnvironmentDriverCreateDTO dto = Instancio.create(EnvironmentDriverCreateDTO.class);

        // when task endpoint gets request, return a response
        EnvironmentCreateResponse environmentCreateResponse = EnvironmentCreateResponse.builder()
                .environmentId("foo")
                .build();
        ServerResponseDTO serverResponse = ServerResponseDTO.builder()
                .body(environmentCreateResponse)
                .state(State.STARTING)
                .build();
        TaskDTO task = TaskDTO.builder().serverResponses(Collections.singletonList(serverResponse)).build();
        Mockito.when(taskEndpoint.getSpecific(environmentDriverCreateAdapter.getRexTaskName(correlationId)))
                .thenReturn(task);

        // when environment driver gets request, return a response
        CompletionStage<EnvironmentCompleteResponse> environmentCompleteResponse = CompletableFuture
                .supplyAsync(() -> Instancio.create(EnvironmentCompleteResponse.class));
        Mockito.when(environmentDriver.cancel(any())).thenReturn(environmentCompleteResponse);
        Mockito.when(environmentDriverProducer.getEnvironmentDriver(any())).thenReturn(environmentDriver);

        // generate stop request
        StopRequest stopRequest = StopRequest.builder().payload(dto).build();

        // send stop request
        environmentDriverCreateAdapter.cancel(correlationId, stopRequest);

        // capture the parameters sent to environment driver
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(environmentDriver).cancel(captor.capture());
        String generated = captor.getValue();

        // verify that the environment id sent to environment driver is generated properly
        assertThat(generated).isEqualTo(environmentCreateResponse.getEnvironmentId());
    }
}
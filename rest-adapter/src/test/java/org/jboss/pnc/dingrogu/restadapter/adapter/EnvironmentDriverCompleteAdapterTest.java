package org.jboss.pnc.dingrogu.restadapter.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;

import org.instancio.Instancio;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCompleteRequest;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCompleteResponse;
import org.jboss.pnc.api.environmentdriver.dto.EnvironmentCreateResponse;
import org.jboss.pnc.dingrogu.api.client.EnvironmentDriver;
import org.jboss.pnc.dingrogu.api.client.EnvironmentDriverProducer;
import org.jboss.pnc.dingrogu.api.dto.adapter.EnvironmentDriverCompleteDTO;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.dto.ServerResponseDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
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
class EnvironmentDriverCompleteAdapterTest {

    @Inject
    EnvironmentDriverCompleteAdapter environmentDriverCompleteAdapter;

    @Inject
    EnvironmentDriverCreateAdapter environmentDriverCreateAdapter;

    @InjectMock
    EnvironmentDriverProducer environmentDriverProducer;

    @Mock
    EnvironmentDriver environmentDriver;

    @InjectMock
    TaskEndpoint taskEndpoint;

    @BeforeEach
    void setupMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getAdapterName() {
        assertThat(environmentDriverCompleteAdapter.getAdapterName()).isNotEmpty();
        assertThat(environmentDriverCompleteAdapter.getAdapterName()).contains("environment");
        assertThat(environmentDriverCompleteAdapter.getAdapterName()).contains("driver");
        assertThat(environmentDriverCompleteAdapter.getAdapterName()).contains("complete");
    }

    @Test
    void start() {
        // generate random DTO
        String correlationId = "123";
        EnvironmentDriverCompleteDTO dto = Instancio.create(EnvironmentDriverCompleteDTO.class);

        // when task endpoint gets request, return a response
        EnvironmentCreateResponse environmentCreateResponse = EnvironmentCreateResponse.builder()
                .environmentId("foo")
                .build();
        ServerResponseDTO serverResponse = ServerResponseDTO.builder().body(environmentCreateResponse).build();
        TaskDTO task = TaskDTO.builder().serverResponses(Collections.singletonList(serverResponse)).build();
        Mockito.when(taskEndpoint.getSpecific(environmentDriverCreateAdapter.getRexTaskName(correlationId)))
                .thenReturn(task);

        // when environment driver gets request, return a response
        CompletionStage<EnvironmentCompleteResponse> environmentCompleteResponse = CompletableFuture
                .supplyAsync(() -> Instancio.create(EnvironmentCompleteResponse.class));
        Mockito.when(environmentDriver.complete(any())).thenReturn(environmentCompleteResponse);
        Mockito.when(environmentDriverProducer.getEnvironmentDriver(any())).thenReturn(environmentDriver);

        // generate start request
        StartRequest startRequest = StartRequest.builder().payload(dto).build();

        // send start request
        environmentDriverCompleteAdapter.start(correlationId, startRequest);

        // capture the parameters sent to environment driver
        ArgumentCaptor<EnvironmentCompleteRequest> captor = ArgumentCaptor.forClass(EnvironmentCompleteRequest.class);
        Mockito.verify(environmentDriver).complete(captor.capture());
        EnvironmentCompleteRequest generated = captor.getValue();

        // verify that the environment complete request sent to environment driver is generated properly
        assertThat(generated.isEnableDebug()).isEqualTo(dto.isDebugEnabled());
        assertThat(generated.getEnvironmentId()).isEqualTo(environmentCreateResponse.getEnvironmentId());
    }
}
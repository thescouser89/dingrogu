package org.jboss.pnc.dingrogu.restadapter.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.instancio.Instancio;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverSealDTO;
import org.jboss.pnc.dingrogu.restadapter.client.RepositoryDriverClient;
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
class RepositoryDriverSealAdapterTest {

    @InjectMock
    RepositoryDriverClient client;

    @InjectMock
    CallbackEndpoint callbackEndpoint;

    @Inject
    RepositoryDriverSealAdapter repositoryDriverSealAdapter;

    @BeforeEach
    void setupMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getAdapterName() {
        assertThat(repositoryDriverSealAdapter.getAdapterName()).isNotEmpty();
        assertThat(repositoryDriverSealAdapter.getAdapterName()).contains("seal");
    }

    @Test
    void start() {
        // given random data
        String correlationId = "testmecorrelation1234";

        // generate random DTO
        RepositoryDriverSealDTO dto = Instancio.create(RepositoryDriverSealDTO.class);
        StartRequest startRequest = StartRequest.builder().payload(dto).build();

        // send request
        repositoryDriverSealAdapter.start(correlationId, startRequest);

        // capture the parameters sent to Causeway
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client).seal(urlCaptor.capture(), captor.capture());

        String payload = captor.getValue();

        assertThat(urlCaptor.getValue()).isEqualTo(dto.getRepositoryDriverUrl());
        assertThat(payload).isEqualTo(dto.getBuildContentId());

        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Mockito.verify(callbackEndpoint).succeed(repositoryDriverSealAdapter.getRexTaskName(correlationId), null, null);
    }
}
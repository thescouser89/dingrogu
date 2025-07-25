package org.jboss.pnc.dingrogu.restadapter.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

import jakarta.inject.Inject;

import org.instancio.Instancio;
import org.jboss.pnc.api.dto.HeartbeatConfig;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryPromoteRequest;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverPromoteDTO;
import org.jboss.pnc.dingrogu.restadapter.client.RepositoryDriverClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RepositoryDriverPromoteAdapterTest {

    @Inject
    RepositoryDriverPromoteAdapter adapter;

    @InjectMock
    RepositoryDriverClient repositoryDriverClient;

    @InjectMock
    CallbackEndpoint callbackEndpoint;

    @Test
    void getAdapterName() {
        assertThat(adapter.getAdapterName()).isNotEmpty();
        assertThat(adapter.getAdapterName()).contains("repository-driver");
    }

    @Test
    void start() {

        // Generate random DTO
        String correlationId = "correlation-hahah";
        RepositoryDriverPromoteDTO dto = Instancio.create(RepositoryDriverPromoteDTO.class);

        HeartbeatConfig heartbeatConfig = Instancio.create(HeartbeatConfig.class);
        StartRequest startRequest = StartRequest.builder().payload(dto).heartbeatConfig(heartbeatConfig).build();
        // send request
        adapter.start(correlationId, startRequest);

        // capture the parameters sent to RepositoryDriverClient
        ArgumentCaptor<RepositoryPromoteRequest> captor = ArgumentCaptor.forClass(RepositoryPromoteRequest.class);
        Mockito.verify(repositoryDriverClient).promote(eq(dto.getRepositoryDriverUrl()), captor.capture());
        RepositoryPromoteRequest generated = captor.getValue();

        // verify that the RepositoryCreateRequest sent to repository driver is generated properly
        assertThat(generated.getBuildContentId()).isEqualTo(dto.getBuildContentId());
        assertThat(generated.getBuildType()).isEqualTo(dto.getBuildType());
        assertThat(generated.getBuildCategory()).isEqualTo(dto.getBuildCategory());
        assertThat(generated.isTempBuild()).isEqualTo(dto.isTempBuild());
        assertThat(generated.getBuildConfigurationId()).isEqualTo(dto.getBuildConfigurationId());
        //        assertThat(generated.getHeartBeat()).isEqualTo(heartbeatConfig.getRequest());
    }
}

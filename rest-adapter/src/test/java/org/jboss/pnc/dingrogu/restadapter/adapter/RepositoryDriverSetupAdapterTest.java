package org.jboss.pnc.dingrogu.restadapter.adapter;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.instancio.Instancio;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateRequest;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateResponse;
import org.jboss.pnc.api.reqour.dto.AdjustResponse;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverSetupDTO;
import org.jboss.pnc.dingrogu.restadapter.client.RepositoryDriverClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
public class RepositoryDriverSetupAdapterTest {

    @Inject
    RepositoryDriverSetupAdapter adapter;

    @Inject
    ReqourAdjustAdapter reqourAdjustAdapter;

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
        RepositoryDriverSetupDTO dto = Instancio.create(RepositoryDriverSetupDTO.class);
        dto.setBuildType(BuildType.MVN.name());

        // create reqour response
        Map<String, Object> pastResults = new HashMap<>();
        AdjustResponse adjustResponse = Instancio.create(AdjustResponse.class);
        pastResults.put(reqourAdjustAdapter.getRexTaskName(correlationId), adjustResponse);

        StartRequest startRequest = StartRequest.builder().payload(dto).taskResults(pastResults).build();
        // when repository driver client get the request, return with a response
        RepositoryCreateResponse response = Instancio.create(RepositoryCreateResponse.class);
        Mockito.when(repositoryDriverClient.setup(any(), any())).thenReturn(response);

        // send request
        adapter.start(correlationId, startRequest);

        // capture the parameters sent to RepositoryDriverClient
        ArgumentCaptor<RepositoryCreateRequest> captor = ArgumentCaptor.forClass(RepositoryCreateRequest.class);
        Mockito.verify(repositoryDriverClient).setup(eq(dto.getRepositoryDriverUrl()), captor.capture());
        RepositoryCreateRequest generated = captor.getValue();

        // verify that the RepositoryCreateRequest sent to repository driver is generated properly
        assertThat(generated.isTempBuild()).isEqualTo(dto.isTempBuild());
        assertThat(generated.getBuildContentId()).isEqualTo(dto.getBuildContentId());
        assertThat(generated.getBuildType().name()).isEqualTo(dto.getBuildType());
        assertThat(generated.isBrewPullActive()).isEqualTo(dto.isBrewPullActive());
        List<String> removedRepositoriesUrl = adjustResponse.getManipulatorResult()
                .getRemovedRepositories()
                .stream()
                .map(repo -> repo.getUrl().toString())
                .toList();
        assertThat(generated.getExtraRepositories()).isEqualTo(removedRepositoriesUrl);

        // wait for 5 seconds for callback endpoint to be triggered
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Mockito.verify(callbackEndpoint).succeed(adapter.getRexTaskName(correlationId), response, null);
    }
}

package org.jboss.pnc.dingrogu.restadapter.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.rex.common.enums.ResponseFlag.SKIP_ROLLBACK;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Set;

import jakarta.inject.Inject;

import org.instancio.Instancio;
import org.jboss.pnc.api.dto.HeartbeatConfig;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.api.reqour.dto.AdjustRequest;
import org.jboss.pnc.api.reqour.dto.AdjustResponse;
import org.jboss.pnc.api.reqour.dto.InternalGitRepositoryUrl;
import org.jboss.pnc.api.reqour.dto.ReqourCallback;
import org.jboss.pnc.dingrogu.api.dto.adapter.ReqourAdjustDTO;
import org.jboss.pnc.dingrogu.restadapter.client.ReqourClient;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ReqourAdjustAdapterTest {

    @Inject
    ReqourAdjustAdapter reqourAdjustAdapter;

    @InjectMock
    ReqourClient reqourClient;

    @InjectMock
    CallbackEndpoint callbackEndpoint;

    @Test
    void getAdapterName() {
        assertThat(reqourAdjustAdapter.getAdapterName()).isNotEmpty();
        assertThat(reqourAdjustAdapter.getAdapterName()).contains("reqour");
        assertThat(reqourAdjustAdapter.getAdapterName()).contains("adjust");
    }

    @Test
    void start() {

        // given random data with a specific scm repo
        String correlationId = "correlationId1234";
        String scmRepo = "git@gitlab.com:project-ncl/dingrogu";
        String internalUrl = "https://gitlab.com/project-ncl/dingrogu";

        // Generate random DTO
        ReqourAdjustDTO dto = Instancio.create(ReqourAdjustDTO.class);

        HeartbeatConfig heartbeatConfig = Instancio.create(HeartbeatConfig.class);
        // set a specific value for scm repo so that it generates expected values
        dto.setScmRepoURL(scmRepo);
        StartRequest startRequest = StartRequest.builder().payload(dto).heartbeatConfig(heartbeatConfig).build();

        // send request
        reqourAdjustAdapter.start(correlationId, startRequest);

        // capture the parameters sent to ReqourClient
        ArgumentCaptor<AdjustRequest> captor = ArgumentCaptor.forClass(AdjustRequest.class);
        Mockito.verify(reqourClient).adjust(eq(dto.getReqourUrl()), captor.capture());
        AdjustRequest generated = captor.getValue();

        // verify that the AdjustRequest sent to reqour is generated properly
        assertThat(generated.getRef()).isEqualTo(dto.getScmRevision());
        assertThat(generated.isTempBuild()).isEqualTo(dto.isTempBuild());
        assertThat(generated.getTaskId()).isEqualTo(dto.getId());
        assertThat(generated.getHeartbeatConfig()).isEqualTo(heartbeatConfig);

        InternalGitRepositoryUrl internal = generated.getInternalUrl();
        assertThat(internal.getReadwriteUrl()).isEqualTo(dto.getScmRepoURL());
        System.out.println(internalUrl);
        System.out.println(internal.getReadonlyUrl());
        System.out.println(internal.getReadwriteUrl());
        assertThat(internal.getReadonlyUrl()).isEqualTo(internalUrl);
    }

    @Test
    void successCallback() {

        // given a success response from reqour
        AdjustResponse adjustResponse = AdjustResponse.builder()
                .callback(ReqourCallback.builder().status(ResultStatus.SUCCESS).build())
                .tag("tag-1234")
                .upstreamCommit("upstream-commit")
                .build();

        String correlationId = "correlationid-1234";
        reqourAdjustAdapter.callback(correlationId, adjustResponse);

        // verify that the successful callback is called
        Mockito.verify(callbackEndpoint)
                .succeed(reqourAdjustAdapter.getRexTaskName(correlationId), adjustResponse, null, null);
    }

    @Test
    void failCallback() {

        // given a bad response from reqour
        AdjustResponse adjustResponse = AdjustResponse.builder()
                .callback(ReqourCallback.builder().status(ResultStatus.FAILED).build())
                .tag("tag-1234")
                .upstreamCommit("upstream-commit")
                .build();

        String correlationId = "correlationid-1234";
        reqourAdjustAdapter.callback(correlationId, adjustResponse);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(reqourAdjustAdapter.getRexTaskName(correlationId), adjustResponse, null, Set.of(SKIP_ROLLBACK));
    }

    @Test
    void systemErrorCallback() {

        // given a bad response from reqour
        AdjustResponse adjustResponse = AdjustResponse.builder()
                .callback(ReqourCallback.builder().status(ResultStatus.SYSTEM_ERROR).build())
                .tag("tag-1234")
                .upstreamCommit("upstream-commit")
                .build();

        String correlationId = "correlationid-1234";
        reqourAdjustAdapter.callback(correlationId, adjustResponse);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(reqourAdjustAdapter.getRexTaskName(correlationId), adjustResponse, null, null);
    }

    @Test
    void noResponseCallback() {

        // given a no response from reqour
        AdjustResponse adjustResponse = null;

        String correlationId = "correlationid-1234";
        reqourAdjustAdapter.callback(correlationId, adjustResponse);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(reqourAdjustAdapter.getRexTaskName(correlationId), adjustResponse, null, null);
    }

    @Test
    void notParseableCallback() {

        // given a bad DTO response from reqour that cannot be parsed to AdjustResponse
        String adjustResponse = "test-me-i-shoul-fail";

        String correlationId = "correlationid-1234";
        reqourAdjustAdapter.callback(correlationId, adjustResponse);

        // verify that the fail callback is called
        Mockito.verify(callbackEndpoint)
                .fail(reqourAdjustAdapter.getRexTaskName(correlationId), adjustResponse, null, null);
    }
}
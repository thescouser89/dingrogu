package org.jboss.pnc.dingrogu.restadapter.rest;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;

import java.net.URL;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.jboss.pnc.dingrogu.api.dto.adapter.DummyDTO;
import org.jboss.pnc.dingrogu.api.dto.dummy.DummyServiceResponseDTO;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.restadapter.adapter.DummyAdapter;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class AdapterEndpointImplTest {

    @InjectMock
    DummyAdapter dummyAdapter;

    @TestHTTPEndpoint(AdapterEndpointImpl.class)
    @TestHTTPResource
    URL url;

    @BeforeEach
    void setup() {
        Mockito.when(dummyAdapter.getAdapterName()).thenReturn("dummy-adapter");
    }

    @Test
    void testStartCalled() {

        String baseUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
        StartRequest startRequest = StartRequest.builder()
                .payload(DummyDTO.builder().dummyServiceUrl(baseUrl + "/dummy-service").build())
                .build();

        Mockito.when(dummyAdapter.start(any(), any())).thenReturn(Optional.of("yes"));

        given().when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(startRequest)
                .post(AdapterEndpoint.getStartAdapterEndpoint(baseUrl, dummyAdapter.getAdapterName(), "1234"))
                .then()
                .statusCode(Response.Status.ACCEPTED.getStatusCode());
    }

    @Test
    void testStartCalledWithNotExistingAdapterShouldFail() {

        String baseUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();

        StartRequest startRequest = StartRequest.builder()
                .payload("Hello")
                .build();

        given().when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(startRequest)
                .post(AdapterEndpoint.getStartAdapterEndpoint(baseUrl, "i-do-not-exist", "1234"))
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void testCancelCalled() {
        String baseUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();

        StopRequest stopRequest = StopRequest.builder()
                .payload("Stop-Hammer-time")
                .build();

        Mockito.doNothing().when(dummyAdapter).cancel(any(), any());

        given().when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(stopRequest)
                .post(AdapterEndpoint.getCancelAdapterEndpoint(baseUrl, dummyAdapter.getAdapterName(), "1234"))
                .then()
                .statusCode(Response.Status.ACCEPTED.getStatusCode());
    }

    @Test
    void testCancelCalledButNoAdapter() {
        String baseUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();

        StopRequest stopRequest = StopRequest.builder()
                .payload("Stop-Hammer-time")
                .build();

        given().when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(stopRequest)
                .post(AdapterEndpoint.getCancelAdapterEndpoint(baseUrl, "idonotexist", "1234"))
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void testCallbackCalled() {
        String baseUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();

        DummyServiceResponseDTO dummyServiceResponseDTO = DummyServiceResponseDTO.builder()
                .status("100")
                .build();

        Mockito.doNothing().when(dummyAdapter).callback(any(), any());

        given().when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(dummyServiceResponseDTO)
                .post(AdapterEndpoint.getCallbackAdapterEndpoint(baseUrl, dummyAdapter.getAdapterName(), "1234"))
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    void testCallbackCalledButNoAdapter() {
        String baseUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();

        DummyServiceResponseDTO dummyServiceResponseDTO = DummyServiceResponseDTO.builder()
                .status("100")
                .build();

        given().when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(dummyServiceResponseDTO)
                .post(AdapterEndpoint.getCallbackAdapterEndpoint(baseUrl, "idonotexist", "1234"))
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }
}
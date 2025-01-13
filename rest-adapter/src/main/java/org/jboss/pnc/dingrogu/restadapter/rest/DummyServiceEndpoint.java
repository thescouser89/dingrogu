package org.jboss.pnc.dingrogu.restadapter.rest;


import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import kong.unirest.core.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.dingrogu.api.dto.dummy.DummyServiceRequestDTO;
import org.jboss.pnc.dingrogu.api.dto.dummy.DummyServiceResponseDTO;

import java.util.concurrent.ExecutorService;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/dummy-service")
@Slf4j
public class DummyServiceEndpoint {

    @Inject
    ExecutorService executorService;

    @POST
    public void startDummyService(DummyServiceRequestDTO dto) {

        executorService.submit(() -> {
            // Sleep so that we run this code after we return success
            try { Thread.sleep(5000L); } catch (InterruptedException e) {}

            DummyServiceResponseDTO reply = DummyServiceResponseDTO.builder().status("OK").build();
            Unirest.post(dto.getCallbackUrl())
                    .header("accept", "application/json")
                    .body(reply)
                    .asJson();
            log.info("Dummy response sent");
        });
    }
}

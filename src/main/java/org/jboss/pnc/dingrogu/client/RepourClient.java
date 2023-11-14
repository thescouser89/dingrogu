package org.jboss.pnc.dingrogu.client;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.pnc.api.repour.dto.RepourCloneRepositoryRequest;
import org.jboss.pnc.api.repour.dto.RepourCreateRepositoryRequest;
import org.jboss.pnc.dingrogu.dto.RepourCloneResponse;
import org.jboss.pnc.dingrogu.dto.RepourCreateRepositoryResponse;

@Path("/")
@RegisterRestClient
public interface RepourClient {

    @POST
    RepourCreateRepositoryResponse createInternalRepository(RepourCreateRepositoryRequest request);

    @POST
    RepourCloneResponse cloneRepository(RepourCloneRepositoryRequest request);
}

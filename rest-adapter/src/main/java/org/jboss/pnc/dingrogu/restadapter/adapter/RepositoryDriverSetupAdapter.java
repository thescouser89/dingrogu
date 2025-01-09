package org.jboss.pnc.dingrogu.restadapter.adapter;

import jakarta.inject.Inject;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryCreateRequest;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverSetupDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourAdjustResponse;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.dingrogu.restadapter.client.RepositoryDriverClient;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class RepositoryDriverSetupAdapter implements Adapter<RepositoryDriverSetupDTO> {

    @Inject
    RepositoryDriverClient repositoryDriverClient;

    @Override
    public String getName() {
        return "repository-driver-setup";
    }

    @Override
    public void start(String correlationId, StartRequest startRequest) {

        // TODO: grab that repository list from Rex previous results:
        List<String> repositoriesToCreate = new ArrayList<>();

        RepositoryDriverSetupDTO repositorySetupDTO = (RepositoryDriverSetupDTO) startRequest.getPayload();
        RepositoryCreateRequest createRequest = new RepositoryCreateRequest(
                repositorySetupDTO.getBuildContentId(),

                BuildType.valueOf(repositorySetupDTO.getBuildType()),
                repositorySetupDTO.isTempBuild(),
                repositorySetupDTO.isBrewPullActive(),
                repositoriesToCreate);

        repositoryDriverClient.setup(repositorySetupDTO.getRepositoryDriverUrl(), createRequest);
    }

    @Override
    public void callback(String correlationId, Object object) {

        RepourAdjustResponse response = (RepourAdjustResponse) object;

        // TODO: send result to Rex via the positive/negative callback
    }

    @Override
    public void cancel(String correlationId, StopRequest stopRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateTaskDTO generateRexTask(
            String adapterUrl,
            String correlationId,
            RepositoryDriverSetupDTO repositorySetupDTO) throws Exception {

        Request startSetup = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repositorySetupDTO);

        Request cancelSetup = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getName(), correlationId)),
                List.of(TaskHelper.getJsonHeader()),
                repositorySetupDTO);

        return CreateTaskDTO.builder()
                .name(getName())
                .remoteStart(startSetup)
                .remoteCancel(cancelSetup)
                .configuration(new ConfigurationDTO())
                .build();
    }
}

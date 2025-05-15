package org.jboss.pnc.dingrogu.restworkflow.rest;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.workflow.BrewPushWorkflowDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildWorkflowClearEnvironmentDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.DeliverablesAnalysisWorkflowDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.DummyWorkflowDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.RepositoryCreationDTO;
import org.jboss.pnc.dingrogu.api.endpoint.WorkflowEndpoint;
import org.jboss.pnc.dingrogu.restadapter.adapter.BuildDriverAdapter;
import org.jboss.pnc.dingrogu.restworkflow.workflows.BrewPushWorkflow;
import org.jboss.pnc.dingrogu.restworkflow.workflows.BuildWorkflow;
import org.jboss.pnc.dingrogu.restworkflow.workflows.DeliverablesAnalysisWorkflow;
import org.jboss.pnc.dingrogu.restworkflow.workflows.DummyWorkflow;
import org.jboss.pnc.dingrogu.restworkflow.workflows.RepositoryCreationWorkflow;
import org.jboss.pnc.rex.api.CallbackEndpoint;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.model.requests.NotificationRequest;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import io.quarkus.logging.Log;

/**
 * Implementation of the workflow endpoint
 *
 * Any new workflow will require the addition of a new endpoint here
 */
@ApplicationScoped
public class WorkflowEndpointImpl implements WorkflowEndpoint {

    @Inject
    BrewPushWorkflow brewPushWorkflow;

    @Inject
    RepositoryCreationWorkflow repositoryCreationWorkflow;

    @Inject
    BuildWorkflow buildWorkflow;

    @Inject
    DeliverablesAnalysisWorkflow deliverablesAnalysisWorkflow;

    @Inject
    DummyWorkflow dummyWorkflow;

    @Inject
    TaskEndpoint taskEndpoint;

    @Inject
    CallbackEndpoint callbackEndpoint;

    @Inject
    BuildDriverAdapter buildDriverAdapter;

    @Inject
    ManagedExecutor managedExecutor;

    @Override
    public CorrelationId startBrewPushWorkflow(BrewPushWorkflowDTO brewPushWorkflowDTO) {
        return brewPushWorkflow.submitWorkflow(brewPushWorkflowDTO);
    }

    @Override
    public Response brewPushNotificationFromRex(NotificationRequest notificationRequest) {
        return brewPushWorkflow.rexNotification(notificationRequest);
    }

    @Override
    public Response repositoryCreationNotificationFromRex(NotificationRequest notificationRequest) {
        return repositoryCreationWorkflow.rexNotification(notificationRequest);
    }

    @Override
    public CorrelationId startRepositoryCreationWorkflow(RepositoryCreationDTO repositoryCreationDTO) {
        return repositoryCreationWorkflow.submitWorkflow(repositoryCreationDTO);
    }

    @Override
    public CorrelationId startBuildWorkflowFromRex(StartRequest startRequest) {
        return buildWorkflow.submitWorkflow(startRequest);
    }

    @Override
    public void buildWorkflowClearEnvironment(BuildWorkflowClearEnvironmentDTO buildWorkflowClearEnvironmentDTO) {

        buildWorkflow.clearEnvironment(buildWorkflowClearEnvironmentDTO);

        // whatever happens, let's just say that it succeeded
        managedExecutor.submit(() -> {
            try {
                // sleep for 5 seconds to make sure that Rex has processed the successful start
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                Log.error(e);
            }
            try {
                callbackEndpoint.rollbackOK(
                        buildDriverAdapter.getRexTaskName(buildWorkflowClearEnvironmentDTO.getCorrelationId()),
                        null,
                        null);
            } catch (Exception e) {
                Log.error("Error happened in callback to rex", e);
                callbackEndpoint.rollbackOK(
                        buildDriverAdapter.getRexTaskName(buildWorkflowClearEnvironmentDTO.getCorrelationId()),
                        null,
                        null);
            }
        });
    }

    @Override
    public Response buildWorkflowNotificationFromRex(NotificationRequest notificationRequest) {
        return buildWorkflow.rexNotification(notificationRequest);
    }

    @Override
    public CorrelationId startDeliverablesAnalysisWorkflow(
            DeliverablesAnalysisWorkflowDTO deliverablesAnalysisWorkflowDTO) {
        return deliverablesAnalysisWorkflow.submitWorkflow(deliverablesAnalysisWorkflowDTO);
    }

    @Override
    public Response deliverablesAnalysisNotificationFromRex(NotificationRequest notificationRequest) {
        return deliverablesAnalysisWorkflow.rexNotification(notificationRequest);
    }

    @Override
    public CorrelationId startDummyWorkflow(DummyWorkflowDTO dummyWorkflowDTO) {
        return dummyWorkflow.submitWorkflow(dummyWorkflowDTO);
    }

    @Override
    public Response dummyNotificationFromRex(NotificationRequest notificationRequest) {
        return dummyWorkflow.rexNotification(notificationRequest);
    }

    @Override
    public Response cancelWorkflow(String correlationId) {
        Log.infof("Cancelling workflow with correlation id %s", correlationId);

        Set<TaskDTO> tasks = taskEndpoint.byCorrelation(correlationId);

        for (TaskDTO task : tasks) {
            if (!task.getState().isFinal()) {
                try (Response response = taskEndpoint.cancel(task.getName())) {
                    if (response.getStatus() != Response.Status.ACCEPTED.getStatusCode()) {
                        Log.warnf("Couldn't cancel Rex task: %s", task.getName());
                    }
                } catch (ClientWebApplicationException e) {
                    Log.warnf("Couldn't cancel Rex task: %s with exception %s", task.getName(), e.getMessage());
                }
            }
        }
        return Response.ok().build();
    }
}

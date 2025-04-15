package org.jboss.pnc.dingrogu.api.endpoint;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.workflow.BrewPushWorkflowDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildWorkDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.DeliverablesAnalysisWorkflowDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.DummyWorkflowDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.RepositoryCreationDTO;
import org.jboss.pnc.rex.model.requests.NotificationRequest;
import org.jboss.pnc.rex.model.requests.StartRequest;

/**
 * WorkflowEndpoint interface. Separating the interface and implementation so that you can potentially create a REST
 * client for the workflow endpoint using the interface only
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
@Tag(name = "Workflow", description = "Workflow manipulation through that endpoint")
public interface WorkflowEndpoint {

    String BREW_PUSH_REX_NOTIFY = "/workflow/brew-push/rex-notify";
    String REPOSITORY_CREATION_REX_NOTIFY = "/workflow/repository-creation/rex-notify";
    String BUILD_REX_NOTIFY = "/workflow/build/rex-notify";
    String DELIVERABLES_ANALYSIS_REX_NOTIFY = "/workflow/deliverables-analysis/rex-notify";
    String DUMMY_REX_NOTIFY = "/workflow/dummy/rex-notify";

    /**
     * Start the brew push workflow
     *
     * @param brewPushWorkflowDTO dto
     * @return DTO of the correlationId
     */
    @Path("/workflow/brew-push/start")
    @POST
    CorrelationId startBrewPushWorkflow(BrewPushWorkflowDTO brewPushWorkflowDTO);

    @Path(BREW_PUSH_REX_NOTIFY)
    @POST
    Response brewPushNotificationFromRex(NotificationRequest notificationRequest);

    /**
     * Start the repository creation workflow
     *
     * @param repositoryCreationDTO dto
     * @return DTO of the correlationId
     */
    @Path("/workflow/repository-creation/start")
    @POST
    CorrelationId startRepositoryCreationWorkflow(RepositoryCreationDTO repositoryCreationDTO);

    @Path(REPOSITORY_CREATION_REX_NOTIFY)
    @POST
    Response repositoryCreationNotificationFromRex(NotificationRequest notificationRequest);

    /**
     * Start the build workflow. The build workflow will be driven from Rex itself, but we leave it here for debug
     * purposes
     *
     * @param buildWorkDTO dto
     * @return DTO of the correlationId
     */
    @Path("/workflow/build/start")
    @POST
    CorrelationId startBuildWorkflow(BuildWorkDTO buildWorkDTO);

    /**
     * Start the build workflow, accepting the Rex's StartRequest DTO
     *
     * @param startRequest Rex startRequest start
     * @return DTO of the correlationId
     */
    @Path("/workflow/build/rex-start")
    @POST
    CorrelationId startBuildWorkflowFromRex(StartRequest startRequest);

    @Path(BUILD_REX_NOTIFY)
    @POST
    Response buildWorkflowNotificationFromRex(NotificationRequest notificationRequest);

    /**
     * Start the deliverables-analysis workflow
     *
     * @param deliverablesAnalysisWorkDTO dto
     * @return DTO of the correlationId
     */
    @Path("/workflow/deliverables-analysis/start")
    @POST
    CorrelationId startDeliverablesAnalysisWorkflow(DeliverablesAnalysisWorkflowDTO deliverablesAnalysisWorkDTO);

    @Path(DELIVERABLES_ANALYSIS_REX_NOTIFY)
    @POST
    Response deliverablesAnalysisNotificationFromRex(NotificationRequest notificationRequest);

    /**
     * Start the dummy workflow
     *
     * @param dummyWorkflowDTO dto
     * @return DTO of the correlationId
     */
    @Path("/workflow/dummy/start")
    @POST
    CorrelationId startDummyWorkflow(DummyWorkflowDTO dummyWorkflowDTO);

    @Path(DUMMY_REX_NOTIFY)
    @POST
    Response dummyNotificationFromRex(NotificationRequest notificationRequest);

    /**
     * Cancel a particular workflow, given its correlationId
     * 
     * @param correlationId: id that identifies the workflow
     */
    @Path("/workflow/id/{correlationId}/cancel")
    @POST
    Response cancelWorkflow(String correlationId);
}
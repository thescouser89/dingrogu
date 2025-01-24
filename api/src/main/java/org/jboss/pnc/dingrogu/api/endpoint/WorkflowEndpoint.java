package org.jboss.pnc.dingrogu.api.endpoint;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.dingrogu.api.dto.workflow.BuildWorkDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.DeliverablesAnalysisWorkflowDTO;
import org.jboss.pnc.dingrogu.api.dto.workflow.BrewPushDTO;
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

    String BUILD_REX_NOTIFY = "/workflow/build/rex-notify";

    /**
     * Start the brew push workflow
     *
     * @param brewPushDTO
     * @return DTO of the correlationId
     */
    @Path("/workflow/brew-push/start")
    @POST
    public CorrelationId startBrewPushWorkflow(BrewPushDTO brewPushDTO);

    /**
     * Start the repository creation workflow
     *
     * @param repositoryCreationDTO
     * @return DTO of the correlationId
     */
    @Path("/workflow/repository-creation/start")
    @POST
    public CorrelationId startRepositoryCreationWorkflow(RepositoryCreationDTO repositoryCreationDTO);

    /**
     * Start the build workflow. The build workflow will be driven from Rex itself, but we leave it here for debug
     * purposes
     *
     * @param buildWorkDTO
     * @return DTO of the correlationId
     */
    @Path("/workflow/build/start")
    @POST
    public CorrelationId startBuildWorkflow(BuildWorkDTO buildWorkDTO);

    /**
     * Start the build workflow, accepting the Rex's StartRequest DTO
     *
     * @param startRequest Rex startRequest start
     * @return DTO of the correlationId
     */
    @Path("/workflow/build/rex-start")
    @POST
    public CorrelationId startBuildWorkflowFromRex(StartRequest startRequest);

    @Path("/workflow/build/rex-notify")
    @POST
    public Response buildWorkflowNotificationFromRex(NotificationRequest notificationRequest);

    /**
     * Start the deliverables-analysis workflow
     *
     * @param deliverablesAnalysisWorkDTO
     * @return DTO of the correlationId
     */
    @Path("/workflow/deliverables-analysis/start")
    @POST
    public CorrelationId startDeliverablesAnalysisWorkflow(DeliverablesAnalysisWorkflowDTO buildCreationDTO);

    /**
     * Start the dummy workflow
     *
     * @param object of whatever you have
     * @return DTO of the correlationId
     */
    @Path("/workflow/dummy/start")
    @POST
    public CorrelationId startDummyWorkflow(DummyWorkflowDTO dummyWorkflowDTO);

    /**
     * Cancel a particular workflow, given its correlationId
     * 
     * @param correlationId: id that identifies the workflow
     */
    @Path("/workflow/id/{correlationId}/cancel")
    @POST
    public Response cancelWorkflow(String correlationId);
}
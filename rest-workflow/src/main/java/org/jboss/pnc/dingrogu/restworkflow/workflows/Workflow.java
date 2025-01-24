package org.jboss.pnc.dingrogu.restworkflow.workflows;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;
import org.jboss.pnc.rex.model.requests.NotificationRequest;

/**
 * Interface for which each workflow class should implement
 *
 * @param <T>: DTO used for the workflow
 */
@ApplicationScoped
public interface Workflow<T> {

    /**
     * Submit the workflow to Rex and return back the correlation id
     *
     * @param t: workflow input
     * @return correlationId
     */
    CorrelationId submitWorkflow(T t) throws WorkflowSubmissionException;

    /**
     * Handle notifications from Rex after a state change. Override for more fancy logic, like notifying the caller that
     * the entire workflow is done successfully/failed with the appropriate Response DTO made up of data from all the
     * adapters in the workflow
     * 
     * @param notificationRequest
     * @return response
     */
    default Response rexNotification(NotificationRequest notificationRequest) {
        Log.infof(
                "[%s] -> [%s] Task: %s",
                notificationRequest.getBefore(),
                notificationRequest.getAfter(),
                notificationRequest.getTask().getName());
        return Response.ok().build();
    }
}

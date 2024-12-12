package org.jboss.pnc.dingrogu.restworkflow.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.dingrogu.api.dto.CorrelationId;

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
}

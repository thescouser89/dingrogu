package org.jboss.pnc.dingrogu.restworkflow.workflows;

/**
 * Exception thrown in the submitWorkflow method in the Workflow interface
 */
public class WorkflowSubmissionException extends RuntimeException {

    public WorkflowSubmissionException(Exception e) {
        super(e);
    }

}

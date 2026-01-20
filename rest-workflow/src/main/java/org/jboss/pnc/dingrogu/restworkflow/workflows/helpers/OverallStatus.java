package org.jboss.pnc.dingrogu.restworkflow.workflows.helpers;

import java.util.Optional;

import org.jboss.pnc.api.enums.orch.CompletionStatus;
import org.jboss.pnc.api.orch.dto.ProcessException;

public class OverallStatus {
    public CompletionStatus completionStatus;
    public Optional<ProcessException> processException = Optional.empty();

    public void set(CompletionStatus completionStatus, String systemErrorMessage) {
        this.completionStatus = completionStatus;
        if (completionStatus.isFailed()) {
            this.processException = Optional.of(new ProcessException(systemErrorMessage));
        }
    }

    public void set(CompletionStatus completionStatus) {
        this.completionStatus = completionStatus;
        this.processException = Optional.empty();
    }
}

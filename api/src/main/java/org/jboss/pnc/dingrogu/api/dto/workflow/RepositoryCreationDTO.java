package org.jboss.pnc.dingrogu.api.dto.workflow;

import org.jboss.pnc.api.enums.JobNotificationType;
import org.jboss.pnc.dto.BuildConfiguration;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Data
@Builder
public class RepositoryCreationDTO {

    public String orchUrl;
    public String reqourUrl;

    public String externalRepoUrl;
    public String ref;
    public boolean preBuildSyncEnabled;
    public JobNotificationType jobNotificationType;
    public BuildConfiguration buildConfiguration;

    // Needed for notification purposes. Perhaps we can switch to operationId in the future
    public String taskId;
}

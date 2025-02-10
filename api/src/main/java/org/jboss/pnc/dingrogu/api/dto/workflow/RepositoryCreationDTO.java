package org.jboss.pnc.dingrogu.api.dto.workflow;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.api.enums.JobNotificationType;
import org.jboss.pnc.dto.BuildConfiguration;

@Jacksonized
@Data
@Builder
public class RepositoryCreationDTO {

    public String orchUrl;
    public String repourUrl;

    public String externalRepoUrl;
    public String ref;
    public boolean preBuildSyncEnabled;
    public JobNotificationType jobNotificationType;
    public BuildConfiguration buildConfiguration;

}

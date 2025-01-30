package org.jboss.pnc.dingrogu.api.dto.workflow;

import lombok.Builder;
import lombok.Data;
import org.jboss.pnc.api.enums.JobNotificationType;

@Data
@Builder
public class RepositoryCreationDTO {

    public String orchUrl;
    public String repourUrl;

    public String externalRepoUrl;
    public String ref;
    public boolean preBuildSyncEnabled;
    public JobNotificationType jobNotificationType;

}

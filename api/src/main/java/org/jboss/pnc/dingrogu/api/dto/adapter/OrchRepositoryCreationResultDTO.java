package org.jboss.pnc.dingrogu.api.dto.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.enums.JobNotificationType;

@Jacksonized
@Data
@Builder
public class OrchRepositoryCreationResultDTO {
    String orchUrl;
    String externalUrl;
    boolean preBuildSyncEnabled;
    Long taskId;
    JobNotificationType jobType;
    BuildConfiguration buildConfiguration;
}

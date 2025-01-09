package org.jboss.pnc.dingrogu.api.dto.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Data
@Builder
public class RepositoryDriverSetupDTO {
    String repositoryDriverUrl;

    String buildContentId;
    String buildType;
    boolean tempBuild;
    boolean brewPullActive;
}

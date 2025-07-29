package org.jboss.pnc.dingrogu.api.dto.adapter;

import java.util.Map;

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
    Map<String, String> genericParameters;
}

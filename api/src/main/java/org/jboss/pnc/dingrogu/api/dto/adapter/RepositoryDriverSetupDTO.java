package org.jboss.pnc.dingrogu.api.dto.adapter;

import java.util.Map;

import org.jboss.pnc.api.enums.BuildCategory;

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
    BuildCategory buildCategory;
    boolean tempBuild;
    boolean brewPullActive;
    Map<String, String> genericParameters;
}

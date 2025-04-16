package org.jboss.pnc.dingrogu.api.dto.adapter;

import org.jboss.pnc.api.enums.BuildCategory;
import org.jboss.pnc.api.enums.BuildType;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Data
@Builder
public class RepositoryDriverPromoteDTO {
    String repositoryDriverUrl;
    String buildContentId;
    BuildType buildType;
    BuildCategory buildCategory;
    boolean tempBuild;
    String buildConfigurationId;
}

package org.jboss.pnc.dingrogu.api.dto.workflow;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourAdjustDTO;

import java.util.Map;

@Jacksonized
@Data
@Builder
public class BuildWorkDTO {
    String repourUrl;
    String scmRepoURL;
    String scmRevision;
    boolean preBuildSyncEnabled;
    String originRepoURL;
    boolean tempBuild;
    String alignmentPreference;
    String id;
    String buildType;
    String defaultAlignmentParams;
    boolean brewPullActive;
    Map<String, String> genericParameters;

    public RepourAdjustDTO toRepourAdjustDTO() {
        return RepourAdjustDTO.builder()
                .repourUrl(repourUrl)
                .scmRepoURL(scmRepoURL)
                .preBuildSyncEnabled(preBuildSyncEnabled)
                .originRepoURL(originRepoURL)
                .tempBuild(tempBuild)
                .alignmentPreference(alignmentPreference)
                .id(id)
                .buildType(buildType)
                .defaultAlignmentParams(defaultAlignmentParams)
                .genericParameters(genericParameters)
                .build();
    }
}

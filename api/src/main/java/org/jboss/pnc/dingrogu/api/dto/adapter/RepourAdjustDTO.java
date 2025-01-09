package org.jboss.pnc.dingrogu.api.dto.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Jacksonized
@Data
@Builder
public class RepourAdjustDTO {
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
}

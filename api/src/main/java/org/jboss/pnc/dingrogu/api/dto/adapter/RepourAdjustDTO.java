package org.jboss.pnc.dingrogu.api.dto.adapter;

import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

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

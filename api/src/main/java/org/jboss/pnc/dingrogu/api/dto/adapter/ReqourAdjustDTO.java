package org.jboss.pnc.dingrogu.api.dto.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.api.enums.AlignmentPreference;
import org.jboss.pnc.api.enums.BuildType;

import java.util.Map;

@Jacksonized
@Data
@Builder
public class ReqourAdjustDTO {
    String reqourUrl;
    String scmRepoURL;
    String scmRevision;
    boolean preBuildSyncEnabled;
    String originRepoURL;
    boolean tempBuild;
    AlignmentPreference alignmentPreference;
    String id;
    BuildType buildType;
    String defaultAlignmentParams;
    boolean brewPullActive;

    Map<String, String> genericParameters;
}

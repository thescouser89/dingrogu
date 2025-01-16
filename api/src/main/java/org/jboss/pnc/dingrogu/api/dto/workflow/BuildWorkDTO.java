package org.jboss.pnc.dingrogu.api.dto.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.api.enums.BuildCategory;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverPromoteDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverSealDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepositoryDriverSetupDTO;
import org.jboss.pnc.dingrogu.api.dto.adapter.RepourAdjustDTO;

import java.util.Map;

@Jacksonized
@Data
@Builder
// TODO: use Mapstruct in the future?
public class BuildWorkDTO {

    String repourUrl;
    String repositoryDriverUrl;

    String scmRepoURL;
    String scmRevision;
    boolean preBuildSyncEnabled;
    String originRepoURL;
    boolean tempBuild;
    String alignmentPreference;
    @NotNull String buildContentId;
    BuildType buildType;
    BuildCategory buildCategory;
    String defaultAlignmentParams;
    boolean brewPullActive;
    Map<String, String> genericParameters;
    String buildConfigurationId;

    public RepourAdjustDTO toRepourAdjustDTO() {
        return RepourAdjustDTO.builder()
                .repourUrl(repourUrl)
                .scmRepoURL(scmRepoURL)
                .scmRevision(scmRevision)
                .preBuildSyncEnabled(preBuildSyncEnabled)
                .originRepoURL(originRepoURL)
                .tempBuild(tempBuild)
                .alignmentPreference(alignmentPreference)
                .id(buildContentId)
                .buildType(buildType.toString())
                .defaultAlignmentParams(defaultAlignmentParams)
                .brewPullActive(brewPullActive)
                .genericParameters(genericParameters)
                .build();
    }

    public RepositoryDriverSetupDTO toRepositoryDriverSetupDTO() {
        return RepositoryDriverSetupDTO.builder()
                .repositoryDriverUrl(repositoryDriverUrl)
                .buildContentId(buildContentId)
                .buildType(buildType.toString())
                .tempBuild(tempBuild)
                .brewPullActive(brewPullActive)
                .build();
    }

    public RepositoryDriverSealDTO toRepositoryDriverSealDTO() {
        return RepositoryDriverSealDTO.builder()
                .repositoryDriverUrl(repositoryDriverUrl)
                .buildContentId(buildContentId)
                .build();

    }

    public RepositoryDriverPromoteDTO toRepositoryDriverPromoteDTO() {
        return RepositoryDriverPromoteDTO.builder()
                .repositoryDriverUrl(repositoryDriverUrl)
                .buildContentId(buildContentId)
                .buildType(buildType)
                .buildCategory(buildCategory)
                .tempBuild(tempBuild)
                .buildConfigurationId(buildConfigurationId)
                .build();

    }
}

package org.jboss.pnc.dingrogu.api.dto.workflow;

import java.util.List;
import java.util.Map;

import org.jboss.pnc.api.enums.AlignmentPreference;
import org.jboss.pnc.enums.BuildType;
import org.jboss.pnc.enums.SystemImageType;
import org.jboss.pnc.spi.executor.BuildExecutionConfiguration;
import org.jboss.pnc.spi.repositorymanager.ArtifactRepository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Super simplified BuildExecutionConfiguration, where the only values we set are for the scm stuff, since this is the
 * only thing that PNC-Orch really extracts from build execution configuration.
 *
 * Hacky? Hell yeah!
 */
@Data
@Jacksonized
@Builder
@AllArgsConstructor
public class BuildExecutionConfigurationSimplifiedDTO implements BuildExecutionConfiguration {
    String scmRepoUrl;
    String scmRevision;
    String scmTag;
    String scmBuildConfigRevision;
    boolean isScmBuildConfigRevisionInternal;

    @Getter
    Map<String, String> user;

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getUserId() {
        return "";
    }

    @Override
    public String getBuildScript() {
        return "";
    }

    @Override
    public String getBuildConfigurationId() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getScmRepoURL() {
        return scmRepoUrl;
    }

    @Override
    public String getScmRevision() {
        return scmRevision;
    }

    @Override
    public String getScmTag() {
        return scmTag;
    }

    @Override
    public String getScmBuildConfigRevision() {
        return scmBuildConfigRevision;
    }

    @Override
    public Boolean isScmBuildConfigRevisionInternal() {
        return isScmBuildConfigRevisionInternal;
    }

    @Override
    public String getOriginRepoURL() {
        return "";
    }

    @Override
    public boolean isPreBuildSyncEnabled() {
        return false;
    }

    @Override
    public String getSystemImageId() {
        return "";
    }

    @Override
    public String getSystemImageRepositoryUrl() {
        return "";
    }

    @Override
    public SystemImageType getSystemImageType() {
        return null;
    }

    @Override
    public boolean isPodKeptOnFailure() {
        return false;
    }

    @Override
    public Map<String, String> getGenericParameters() {
        return Map.of();
    }

    @Override
    public String getDefaultAlignmentParams() {
        return "";
    }

    @Override
    public AlignmentPreference getAlignmentPreference() {
        return null;
    }

    @Override
    public String getBuildContentId() {
        return "";
    }

    @Override
    public boolean isTempBuild() {
        return false;
    }

    @Override
    public boolean isBrewPullActive() {
        return false;
    }

    @Override
    public BuildType getBuildType() {
        return null;
    }

    @Override
    public String getTempBuildTimestamp() {
        return "";
    }

    @Override
    public List<ArtifactRepository> getArtifactRepositories() {
        return List.of();
    }
}

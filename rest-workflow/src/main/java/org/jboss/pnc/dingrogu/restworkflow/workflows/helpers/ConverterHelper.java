package org.jboss.pnc.dingrogu.restworkflow.workflows.helpers;

import java.util.Collections;
import java.util.List;

import org.jboss.pnc.api.enums.ArtifactQuality;
import org.jboss.pnc.api.enums.BuildCategory;
import org.jboss.pnc.api.repositorydriver.dto.RepositoryArtifact;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.TargetRepository;
import org.jboss.pnc.enums.RepositoryType;

public class ConverterHelper {

    public static List<Artifact> convertFromRepositoryArtifacts(List<RepositoryArtifact> artifacts) {
        if (artifacts == null) {
            return Collections.emptyList();
        }
        return artifacts.stream()
                .map(
                        ra -> Artifact.builder()
                                .identifier(ra.getIdentifier())
                                .purl(ra.getPurl())
                                .artifactQuality(convertArtifactQuality(ra.getArtifactQuality()))
                                .buildCategory(convertBuildCategory(ra.getBuildCategory()))
                                .md5(ra.getMd5())
                                .sha1(ra.getSha1())
                                .sha256(ra.getSha256())
                                .filename(ra.getFilename())
                                .deployPath(ra.getDeployPath())
                                .importDate(ra.getImportDate())
                                .originUrl(ra.getOriginUrl())
                                .size(ra.getSize())
                                .targetRepository(convertTargetRepository(ra.getTargetRepository()))
                                .build())
                .toList();
    }

    private static org.jboss.pnc.enums.ArtifactQuality convertArtifactQuality(ArtifactQuality quality) {
        return quality == null ? null : org.jboss.pnc.enums.ArtifactQuality.valueOf(quality.name());
    }

    private static TargetRepository convertTargetRepository(
            org.jboss.pnc.api.repositorydriver.dto.TargetRepository targetRepository) {
        if (targetRepository == null) {
            return null;
        }
        return TargetRepository.refBuilder()
                .temporaryRepo(targetRepository.getTemporaryRepo())
                .identifier(targetRepository.getIdentifier())
                .repositoryType(convertRepositoryType(targetRepository.getRepositoryType()))
                .repositoryPath(targetRepository.getRepositoryPath())
                .build();
    }

    private static RepositoryType convertRepositoryType(org.jboss.pnc.api.enums.RepositoryType repositoryType) {
        if (repositoryType == null) {
            return null;
        }
        return RepositoryType.valueOf(repositoryType.name());
    }

    private static org.jboss.pnc.enums.BuildCategory convertBuildCategory(BuildCategory buildCategory) {
        if (buildCategory == null) {
            return null;
        }
        return org.jboss.pnc.enums.BuildCategory.valueOf(buildCategory.name());
    }
}

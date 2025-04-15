package org.jboss.pnc.dingrogu.api.dto.adapter;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Callback DTO from Repour
 *
 * TODO: move it to pnc-api?
 */
@Jacksonized
@Data
@Builder
public class RepourAdjustResponse {

    public static class RepourUrl {
        @JsonProperty("readwrite")
        String readWrite;

        @JsonProperty("readonly")
        String readOnly;
    }

    public static class AdjustResultData {
        @JsonProperty("VersioningState")
        VersioningState versioningState;

        @JsonProperty("RemovedRepositories")
        List<String> removedRepositories;
    }

    public static class VersioningState {
        @JsonProperty("executionRootModified")
        public ExecutionRootModified executionRootModified;
    }

    public static class ExecutionRootModified {
        String groupId;
        String artifactId;
        String version;
    }

    String tag;
    String commit;
    RepourUrl url;

    @JsonProperty("upstream_commit")
    String upstreamCommit;

    @JsonProperty("is_ref_revision_internal")
    boolean isRefRevisionInternal;

    @JsonProperty("RemovedRepositories")
    List<String> removeRepositories;
}
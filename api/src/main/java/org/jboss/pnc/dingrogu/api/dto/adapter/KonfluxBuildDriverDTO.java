package org.jboss.pnc.dingrogu.api.dto.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Data
@Builder
public class KonfluxBuildDriverDTO {

    String konfluxBuildDriver;

    String buildContentId;

    String scmUrl;
    String scmRevision;
    String buildScript;
    String buildTool;

    String recipeImage;
    String podMemoryOverride;

    String deployUrl;
    String dependenciesUrl;

    String namespace;
    String buildToolVersion;
    String javaVersion;
}

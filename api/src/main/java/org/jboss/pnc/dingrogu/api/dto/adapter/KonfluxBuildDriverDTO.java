package org.jboss.pnc.dingrogu.api.dto.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Data
@Builder
public class KonfluxBuildDriverDTO {

    String konfluxBuildDriverUrl;

    String buildContentId;

    String buildScript;
    String buildTool;

    String recipeImage;
    String podMemoryOverride;

    String namespace;
    String buildToolVersion;
    String javaVersion;
}

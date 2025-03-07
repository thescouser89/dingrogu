package org.jboss.pnc.dingrogu.api.dto.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Data
@Builder
public class EnvironmentDriverCreateDTO {
    String environmentDriverUrl;
    String environmentLabel;
    String environmentImage;
    String buildContentId;
    String podMemoryOverride;
    boolean debugEnabled;
    String buildConfigId;
}

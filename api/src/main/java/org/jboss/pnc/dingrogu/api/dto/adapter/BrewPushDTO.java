package org.jboss.pnc.dingrogu.api.dto.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
@Builder
public class BrewPushDTO {
    private String causewayUrl;
    private String buildId;
    private String tagPrefix;
    private String username;
    private boolean reimport;
}

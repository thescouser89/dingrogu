package org.jboss.pnc.dingrogu.api.dto.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
@Builder
public class OrchBuildPushResultDTO {

    String operationId;
    String buildId;
    String orchUrl;
}

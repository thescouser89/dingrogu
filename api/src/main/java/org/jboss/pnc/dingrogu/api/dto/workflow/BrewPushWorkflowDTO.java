package org.jboss.pnc.dingrogu.api.dto.workflow;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
public class BrewPushWorkflowDTO {

    String causewayUrl;
    String orchUrl;
    String buildId;
    String tagPrefix;
    String username;
    boolean reimport;
    String operationId;
}

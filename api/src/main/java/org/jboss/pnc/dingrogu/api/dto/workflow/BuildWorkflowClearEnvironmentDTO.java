package org.jboss.pnc.dingrogu.api.dto.workflow;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Data
@Builder
public class BuildWorkflowClearEnvironmentDTO {
    String environmentDriverUrl;
    String correlationId;
}

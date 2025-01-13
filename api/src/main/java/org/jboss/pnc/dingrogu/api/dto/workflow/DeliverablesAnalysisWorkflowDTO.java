package org.jboss.pnc.dingrogu.api.dto.workflow;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.dingrogu.api.dto.adapter.DeliverablesAnalyzerDTO;

@Jacksonized
@Data
@Builder
public class DeliverablesAnalysisWorkflowDTO {
    DeliverablesAnalyzerDTO deliverablesAnalyzer;
}

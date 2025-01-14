package org.jboss.pnc.dingrogu.api.dto.workflow;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Jacksonized
@Data
@Builder
public class DeliverablesAnalysisWorkflowDTO {
    String deliverablesAnalyzerUrl;
    String orchUrl;
    String operationId;
    boolean scratch;

    List<String> urls;
    String config;
}

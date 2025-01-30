package org.jboss.pnc.dingrogu.api.dto.workflow;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.jboss.pnc.api.dto.Request;

import java.util.List;

@Jacksonized
@Data
@Builder
public class DeliverablesAnalysisWorkflowDTO {
    String deliverablesAnalyzerUrl;
    String orchUrl;

    List<String> urls;
    String config;
    boolean scratch;

    String operationId;
    // callback for operationId
    Request callback;
}

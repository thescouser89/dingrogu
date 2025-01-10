package org.jboss.pnc.dingrogu.api.dto.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Jacksonized
@Data
@Builder
public class DeliverablesAnalyzerDTO {
    String deliverablesAnalyzerUrl;

    String operationId;
    List<String> urls;
    String config;

}

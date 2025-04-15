package org.jboss.pnc.dingrogu.api.dto.adapter;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Data
@Builder
public class DeliverablesAnalyzerDTO {
    String deliverablesAnalyzerUrl;

    String operationId;
    List<String> urls;
    String config;

}

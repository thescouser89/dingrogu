package org.jboss.pnc.dingrogu.api.dto.adapter;

import org.jboss.pnc.api.dto.Request;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * This DTO represents the response when Deliverables Analyze returns for the analyze endpoint
 *
 * From DelA's AnalyzeResponse, but I didn't want to import the dela jars into dingrogu just for that one DTO
 */
@Jacksonized
@Data
@Builder
public class DelAAnalyzeResponse {

    /**
     * Analysis ID
     */
    private String id;

    /**
     * Request definition to cancel this running analysis
     */
    private Request cancelRequest;
}

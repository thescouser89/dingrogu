package org.jboss.pnc.dingrogu.api.dto.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Data
@Builder
public class RepourCreateRepoResponse {
    String status;

    @JsonProperty("readonly_url")
    String readonlyUrl;

    @JsonProperty("readwrite_url")
    String readwriteUrl;
}

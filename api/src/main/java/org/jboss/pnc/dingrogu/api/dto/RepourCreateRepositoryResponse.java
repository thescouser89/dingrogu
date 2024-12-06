package org.jboss.pnc.dingrogu.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Data
public class RepourCreateRepositoryResponse {

    RepourCreateRepositoryStatus status;

    @JsonProperty("readonly_url")
    String readonlyUrl;

    @JsonProperty("readwrite_url")
    String readwriteUrl;

    @JsonProperty("exit_status")
    int exitStatus;

    @JsonProperty("command_log")
    String commandLog;
}

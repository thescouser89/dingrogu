package org.jboss.pnc.dingrogu.common;

import jakarta.ws.rs.core.MediaType;
import org.jboss.pnc.api.dto.Request;

public class TaskHelper {

    private static final Request.Header JSON_HEADER = new Request.Header("Content-Type", MediaType.APPLICATION_JSON);

    public static Request.Header getJsonHeader() {
        return JSON_HEADER;
    }
}

package org.jboss.pnc.dingrogu.common;

import jakarta.ws.rs.core.MediaType;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.common.log.MDCUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskHelper {

    private static final Request.Header JSON_HEADER = new Request.Header("Content-Type", MediaType.APPLICATION_JSON);
    private static final Request.Header ACCEPT_JSON_HEADER = new Request.Header("Accept", MediaType.APPLICATION_JSON);

    public static List<Request.Header> getHTTPHeaders() {

        List<Request.Header> headers = new ArrayList<>();
        headers.add(JSON_HEADER);
        headers.add(ACCEPT_JSON_HEADER);

        Map<String, String> mdcMap = MDCUtils.getHeadersFromMDC();
        if (mdcMap != null) {
            mdcMap.forEach((key, value) -> headers.add(new Request.Header(key, value)));
        }

        return headers;
    }
}
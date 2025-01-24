package org.jboss.pnc.dingrogu.restadapter.client;

import io.quarkus.oidc.client.Tokens;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.common.log.MDCUtils;

import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

@Slf4j
public class ClientHelper {
    /**
     * Generate a map of key / value for HTTP clients, including the MDC values and authentication needed
     * 
     * @param tokens
     * @return
     */
    public static Map<String, String> getClientHeaders(Tokens tokens) {

        Map<String, String> headers = MDCUtils.getHeadersFromMDC();
        headers.put(AUTHORIZATION, "Bearer " + tokens.getAccessToken());
        return headers;
    }
}

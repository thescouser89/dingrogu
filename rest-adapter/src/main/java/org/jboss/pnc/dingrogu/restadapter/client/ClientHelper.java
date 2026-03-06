package org.jboss.pnc.dingrogu.restadapter.client;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

import java.util.Map;

import org.jboss.pnc.common.log.MDCUtils;
import org.jboss.pnc.quarkus.client.auth.runtime.PNCClientAuth;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientHelper {
    /**
     * Generate a map of key / value for HTTP clients, including the MDC values and authentication needed
     * 
     * @param tokens
     * @return
     */
    public static Map<String, String> getClientHeaders(PNCClientAuth pncClientAuth) {

        Map<String, String> headers = MDCUtils.getHeadersFromMDC();
        headers.put(AUTHORIZATION, pncClientAuth.getHttpAuthorizationHeaderValueWithCachedToken());
        return headers;
    }
}

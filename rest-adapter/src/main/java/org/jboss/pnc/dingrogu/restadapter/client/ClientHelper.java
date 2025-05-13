package org.jboss.pnc.dingrogu.restadapter.client;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

import java.util.Map;

import org.jboss.pnc.common.log.MDCUtils;

import io.quarkus.logging.Log;
import io.quarkus.oidc.client.Tokens;
import lombok.extern.slf4j.Slf4j;

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
        Log.info("Logging client header");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            Log.infof("Header: %s :: Value: %s", entry.getKey(), entry.getValue());
        }
        headers.put(AUTHORIZATION, "Bearer " + tokens.getAccessToken());
        return headers;
    }
}

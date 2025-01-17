package org.jboss.pnc.dingrogu.application.runtime;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.api.constants.MDCHeaderKeys;
import org.jboss.pnc.api.constants.MDCKeys;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Written by Matej Lazar. Copied from the repository-driver codebase
 *
 * This interceptor will automatically load the MDC values
 */
@Provider
@Slf4j
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String REQUEST_EXECUTION_START = "request-execution-start";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        MDC.clear();

        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        headers.forEach((key, value) -> {
            if (!key.toLowerCase().equals("authorization")) {
                log.info("Current header -> {}::{}", key, value);
            }
        });

        UriInfo uriInfo = requestContext.getUriInfo();
        Request request = requestContext.getRequest();
        log.info("== Request {} {}.", request.getMethod(), uriInfo.getRequestUri());
        Map<String, String> mdcContext = getContextMap();
        headerToMap(mdcContext, MDCHeaderKeys.PROCESS_CONTEXT, requestContext);
        headerToMap(mdcContext, MDCHeaderKeys.TMP, requestContext);
        headerToMap(mdcContext, MDCHeaderKeys.EXP, requestContext);
        headerToMap(mdcContext, MDCHeaderKeys.USER_ID, requestContext);
        headerToMap(mdcContext, MDCHeaderKeys.TRACE_ID, requestContext);
        headerToMap(mdcContext, MDCHeaderKeys.SPAN_ID, requestContext);
        headerToMap(mdcContext, MDCHeaderKeys.PARENT_ID, requestContext);
        MDC.setContextMap(mdcContext);

        requestContext.setProperty(REQUEST_EXECUTION_START, System.currentTimeMillis());

        log.info("Requested {} {}.", request.getMethod(), uriInfo.getRequestUri());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Long startTime = (Long) requestContext.getProperty(REQUEST_EXECUTION_START);

        String took;
        if (startTime == null) {
            took = "-1";
        } else {
            took = Long.toString(System.currentTimeMillis() - startTime);
        }
        log.info("Completed {} with status: {}", requestContext.getUriInfo().getPath(), responseContext.getStatus());

        try (MDC.MDCCloseable mdcTook = MDC.putCloseable(MDCKeys.REQUEST_TOOK, took);
                MDC.MDCCloseable mdcStatus = MDC
                        .putCloseable(MDCKeys.RESPONSE_STATUS, Integer.toString(responseContext.getStatus()));) {
            log.debug("Completed {}, took: {}ms.", requestContext.getUriInfo().getPath(), took);
        }
    }

    private void headerToMap(
            Map<String, String> mdcContext,
            MDCHeaderKeys headerKeys,
            ContainerRequestContext requestContext) {
        String value = requestContext.getHeaderString(headerKeys.getHeaderName());
        log.info("Adding {}: {} to mdc", headerKeys.getHeaderName(), value);
        mdcContext.put(headerKeys.getMdcKey(), value);
    }

    private void headerToMap(
            Map<String, String> map,
            MDCHeaderKeys headerKeys,
            ContainerRequestContext requestContext,
            Supplier<String> defaultValue) {

        String value = requestContext.getHeaderString(headerKeys.getHeaderName());

        if (value == null || value.trim().isEmpty()) {
            log.info("Adding default {}: {} to mdc", headerKeys.getHeaderName(), defaultValue.get());
            map.put(headerKeys.getMdcKey(), defaultValue.get());
        } else {
            log.info("Adding {}: {} to mdc", headerKeys.getHeaderName(), value);
            map.put(headerKeys.getMdcKey(), value);
        }
    }

    private static Map<String, String> getContextMap() {
        Map<String, String> context = MDC.getCopyOfContextMap();
        if (context == null) {
            context = new HashMap<>();
        }
        return context;
    }
}

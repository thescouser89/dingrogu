package org.jboss.pnc.dingrogu.common;

import org.jboss.pnc.api.constants.MDCHeaderKeys;
import org.jboss.pnc.api.dto.Request;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TaskHelperTest {

    @Test
    void getHTTPHeadersBasic() {
        List< Request.Header> headers = TaskHelper.getHTTPHeaders();

        Optional<Request.Header> headerContentType = headers.stream().filter(header -> header.getName().equals("Content-Type")).findAny();
        assertThat(headerContentType).isPresent();

        Optional<Request.Header> headerAccept = headers.stream().filter(header -> header.getName().equals("Accept")).findAny();
        assertThat(headerAccept).isPresent();
    }

    @Test
    void getHttpHeadersWithMdc() {
        Map<String, String> contextMap = Map.of(MDCHeaderKeys.USER_ID.getMdcKey(), "dustin", MDCHeaderKeys.PROCESS_CONTEXT.getMdcKey(), "process-context");
        MDC.setContextMap(contextMap);
        List< Request.Header> headers = TaskHelper.getHTTPHeaders();
        Optional<Request.Header> headerUserId = headers.stream().filter(header -> header.getName().equals(MDCHeaderKeys.USER_ID.getHeaderName())).findAny();
        assertThat(headerUserId).isPresent();
        assertThat(headerUserId.get().getValue()).isEqualTo("dustin");

        Optional<Request.Header> headerProcessContext = headers.stream().filter(header -> header.getName().equals(MDCHeaderKeys.PROCESS_CONTEXT.getHeaderName())).findAny();
        assertThat(headerProcessContext).isPresent();
        assertThat(headerProcessContext.get().getValue()).isEqualTo("process-context");
    }
}
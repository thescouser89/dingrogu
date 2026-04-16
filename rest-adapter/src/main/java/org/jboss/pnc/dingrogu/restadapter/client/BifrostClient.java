package org.jboss.pnc.dingrogu.restadapter.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.api.bifrost.dto.Checksums;
import org.jboss.pnc.dingrogu.common.TaskHelper;

import io.quarkus.logging.Log;
import kong.unirest.core.ContentType;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;

@ApplicationScoped
public class BifrostClient {
    public static final String CHECKSUMS_ENDPOINT_TEMPLATE = "/final-log/%s/%s/checksums";
    public static final String GET_FINALLOGS_ENDPOINT_TEMPLATE = "/final-log/%s/%s";

    @Retry
    public Optional<Checksums> getChecksums(String bifrostUrl, String buildId, String tag) {
        Log.infof("Bifrost checksums request for: %s and tag: %s", buildId, tag);
        HttpResponse<Checksums> response = Unirest.get(bifrostUrl + CHECKSUMS_ENDPOINT_TEMPLATE.formatted(buildId, tag))
                .accept(ContentType.APPLICATION_JSON)
                .asObject(Checksums.class);

        return switch (response.getStatus()) {
            case 200 -> Optional.of(response.getBody());

            // some error cases do not produce all tags, therefore 204/404 should not throw exception
            case 204, 404 -> {
                TaskHelper.LIVE_LOG
                        .warn(
                                "Bifrost missing {} logs: HTTP {}, body: {}",
                                tag,
                                response.getStatus(),
                                response.getBody() == null ? response.getStatusText() : response.getBody());
                yield Optional.empty();
            }
            default -> {
                TaskHelper.LIVE_LOG
                        .error(
                                "Request didn't go through: HTTP {}, body: {}",
                                response.getStatus(),
                                response.getBody() == null ? response.getStatusText() : response.getBody());
                throw new RuntimeException("Request didn't go through");
            }
        };
    }

    public URI generateGetFinalLogsURL(String bifrostUrl, String buildId, String tag) {
        try {
            buildId = buildId.replaceFirst("build-", "");

            return new URI(bifrostUrl + GET_FINALLOGS_ENDPOINT_TEMPLATE.formatted(buildId, tag));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}

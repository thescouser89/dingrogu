package org.jboss.pnc.dingrogu.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.filter.OidcClientRequestFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import okhttp3.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.pnc.rex.dto.requests.FinishRequest;

import java.net.URI;

@ApplicationScoped
public class RexClient {
    public static final OkHttpClient CLIENT = new OkHttpClient();

    @ConfigProperty(name = "rexclient.url")
    String rexClientUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Tokens token;

    @Produces
    public RexTaskEndpoint getRexTaskEndpoint() {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(rexClientUrl))
                .register(OidcClientRequestFilter.class)
                .build(RexTaskEndpoint.class);
    }

    public void invokeCallback(String callbackUrl, FinishRequest finishRequest) throws Exception {
        MediaType json = MediaType.get("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(json, objectMapper.writeValueAsString(finishRequest));
        Request request = new Request.Builder().url(callbackUrl)
                .post(requestBody)
                .addHeader("Authentication", "Bearer " + token.getAccessToken())
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            Log.info(response.body().string());
        }
    }
}

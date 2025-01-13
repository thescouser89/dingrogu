package org.jboss.pnc.dingrogu.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import okhttp3.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.dto.requests.FinishRequest;
import org.jboss.pnc.rex.api.TaskEndpoint;

import java.net.URI;

/**
 * I don't know if this RexClient will be replaced by something else in the future, but keeping it for now until
 * something better comes along
 */
@ApplicationScoped
public class RexClient {
    public static final OkHttpClient CLIENT = new OkHttpClient();

    @ConfigProperty(name = "rexclient.url")
    String rexClientUrl;

    @Inject
    ObjectMapper objectMapper;

    // @Inject
    // Tokens token;

    @Produces
    public TaskEndpoint getRexTaskEndpoint() {
        return RestClientBuilder.newBuilder().baseUri(URI.create(rexClientUrl)).build(TaskEndpoint.class);
    }

    public void submitWorkflow(CreateGraphRequest createGraphRequest) throws Exception {

        String url = rexClientUrl + "/rest/tasks";

        MediaType json = MediaType.get("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(json, objectMapper.writeValueAsString(createGraphRequest));
        Log.info("About to submit: " + objectMapper.writeValueAsString(createGraphRequest));

        Request request = new Request.Builder().url(url)
                .post(requestBody)
                // .addHeader("Authentication", "Bearer " + token.getAccessToken())
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            Log.info(response.body().string());
        }

    }
    public void invokeSuccessCallback(String taskName, Object object) throws Exception {
        String url = rexClientUrl + "/rest/callback/" + taskName + "/succeed";

        MediaType json = MediaType.get("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(json, objectMapper.writeValueAsString(object));
        Log.info("About to submit callback: " + objectMapper.writeValueAsString(object));
        Request request = new Request.Builder().url(url)
                .post(requestBody)
                // .addHeader("Authentication", "Bearer " + token.getAccessToken())
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            Log.info(response.body().string());
        }
    }
}

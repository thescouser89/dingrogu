package org.jboss.pnc.dingrogu.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import okhttp3.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.pnc.rex.dto.ServerResponseDTO;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.jboss.pnc.rex.api.TaskEndpoint;

import java.io.IOException;
import java.net.URI;
import java.util.List;

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

    @Inject
    Tokens token;

    @Inject
    OidcClient oidcClient;

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
                .addHeader("Authorization", "Bearer " + token.getAccessToken())
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            Log.info(response.message());
            Log.info("Is successful? " + response.isSuccessful());
            Log.info(response.toString());
            Log.info(response.body().string());
        }

    }

    /**
     * ActivateReqestContext annotation added since this method may be called inside an executorservice with no context
     * propagated. This annotation helps with the tokens requestscoped.
     *
     * @param taskName
     * @param object
     * @throws Exception
     */
    @ActivateRequestContext
    public void invokeSuccessCallback(String taskName, Object object) throws Exception {
        invokeCallback(taskName, object, true);
    }

    /**
     * ActivateReqestContext annotation added since this method may be called inside an executorservice with no context
     * propagated. This annotation helps with the tokens requestscoped.
     *
     * @param taskName
     * @param object
     * @throws Exception
     */
    @ActivateRequestContext
    public void invokeFailCallback(String taskName, Object object) throws Exception {
        invokeCallback(taskName, object, false);
    }

    /**
     * ActivateReqestContext annotation added since this method may be called inside an executorservice with no context
     * propagated. This annotation helps with the tokens requestscoped.
     * 
     * @param taskName
     * @param object
     * @throws Exception
     */
    private void invokeCallback(String taskName, Object object, boolean success) throws Exception {

        String endpoint = "/fail";
        if (success) {
            endpoint = "/succeed";
        }

        Log.info("Callback to Rex being sent for: " + taskName);
        String url = rexClientUrl + "/rest/callback/" + taskName + endpoint;

        MediaType json = MediaType.get("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(json, objectMapper.writeValueAsString(object));
        Log.info("URL: " + url);
        Log.info("About to submit callback: " + objectMapper.writeValueAsString(object));
        Request request = new Request.Builder().url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + token.getAccessToken())
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            Log.info(response.message());
            Log.info("Is successful? " + response.isSuccessful());
            Log.info(response.toString());
            Log.info(response.body().string());
        }
    }

    /**
     * Get Rex task result
     *
     * @param taskName
     * @param clazz DTO to cast the result to
     * @return
     */
    public <T> T getTaskResponse(String taskName, Class<T> clazz) {
        String url = rexClientUrl + "/rest/tasks/" + taskName;

        MediaType json = MediaType.get("application/json; charset=utf-8");
        Request request = new Request.Builder().url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token.getAccessToken())
                .build();
        try (Response response = CLIENT.newCall(request).execute()) {
            Log.info(response.message());
            Log.info("Is successful? " + response.isSuccessful());
            TaskDTO dto = objectMapper.readValue(response.body().string(), TaskDTO.class);
            List<ServerResponseDTO> responses = dto.getServerResponses();

            // get last index
            ServerResponseDTO serverResponseDTO = responses.get(responses.size() - 1);
            return objectMapper.convertValue(serverResponseDTO.getBody(), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

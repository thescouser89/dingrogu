package org.jboss.pnc.dingrogu.restadapter.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.NotificationRequest;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import java.net.URI;

/**
 * Interface for all the adapters
 *
 * <T>: generate rex task DTO
 */
@ApplicationScoped
public interface Adapter<T> {

    /**
     * Translates the Rex StartRequest DTO to the application's DTO and send the request to the application
     *
     * @param startRequest
     */
    void start(String correlationId, StartRequest startRequest);

    /**
     * Translates the callback from the application's DTO and send it back to Rex
     *
     * @param object callback object
     */
    void callback(String correlationId, Object object);

    /**
     * Send cancel request from Rex to the application
     *
     * @param stopRequest
     */
    void cancel(String correlationId, StopRequest stopRequest);

    /**
     * Get name of the adapter. The same name will be used for the adapter REST endpoint path and for the name of the
     * Rex task
     *
     * @return name of the adapter and the name of the Rex task
     */
    String getAdapterName();

    /**
     * Indicate whether we want to get results from dependencies. Override the default method if yes, you should
     * 
     * @return boolean
     */
    default boolean shouldGetResultsFromDependencies() {
        return false;
    }

    /**
     * Override this method if we want to send a request to an endpoint for a failed task in the workflow By default
     * nothing will be sent. See {@link AdapterEndpoint#rexNotification(NotificationRequest)}
     * 
     * @return Request
     */
    default Request failedTaskNotification() {
        return null;
    }

    /**
     * Get the rex task name that we'll submit to Rex. We prepend the correlation id to it to make the Rex task name
     * unique
     *
     * @param correlationId correlation id
     * @return Name of the Rex task
     */
    default String getRexTaskName(String correlationId) {
        return correlationId + "-" + getAdapterName();
    }

    /**
     * Get the notification endpoint after a Rex task transitions to another state. Override this method to set a
     * different notification endpoint from the default one.
     * 
     * @param adapterUrl
     * @return
     */
    default String getNotificationEndpoint(String adapterUrl) {
        return AdapterEndpoint.getNotificationEndpoint(adapterUrl);
    }

    /**
     * Generate the Rex Task DTO. That Rex task should communicate to the adapter endpoint
     *
     * @param adapterUrl
     * @param correlationId
     * @param notificationAttachment
     * @param t data needed to generate the rex task
     * @return Rex task
     * @throws Exception if something went wrong
     */
    default CreateTaskDTO generateRexTask(String adapterUrl, String correlationId, Object notificationAttachment, T t)
            throws Exception {

        Request startAdjust = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getStartAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                TaskHelper.getHTTPHeaders(),
                t);

        Request cancelAdjust = new Request(
                Request.Method.POST,
                new URI(AdapterEndpoint.getCancelAdapterEndpoint(adapterUrl, getAdapterName(), correlationId)),
                TaskHelper.getHTTPHeaders(),
                t);

        Request callerNotification = new Request(
                Request.Method.POST,
                new URI(getNotificationEndpoint(adapterUrl)),
                TaskHelper.getHTTPHeaders(),
                notificationAttachment);

        // TODO: I'm not really sure if the passMDCInRequestBody does anything here?
        return CreateTaskDTO.builder()
                .name(getRexTaskName(correlationId))
                .remoteStart(startAdjust)
                .remoteCancel(cancelAdjust)
                .callerNotifications(callerNotification)
                .configuration(
                        ConfigurationDTO.builder()
                                .passResultsOfDependencies(shouldGetResultsFromDependencies())
                                .build())
                .build();
    }
}

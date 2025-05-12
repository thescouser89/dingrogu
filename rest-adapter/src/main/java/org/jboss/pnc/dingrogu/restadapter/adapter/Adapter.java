package org.jboss.pnc.dingrogu.restadapter.adapter;

import java.net.URI;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.dingrogu.api.endpoint.AdapterEndpoint;
import org.jboss.pnc.dingrogu.common.TaskHelper;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.dto.CreateTaskDTO;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

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
     * You can optionally return an object that will be sent to the sender (Rex). This is useful if you want to return a
     * unique id that the start method has obtained from the application
     * 
     * @param startRequest
     */
    Optional<Object> start(String correlationId, StartRequest startRequest);

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
     * Get the notification endpoint after a Rex task transitions to another state.
     *
     * @param adapterUrl
     * @return
     */
    String getNotificationEndpoint(String adapterUrl);

    /**
     * Indicate whether we want to get results from dependencies. Override the default method if yes, you should
     * 
     * @return boolean
     */
    default boolean shouldGetResultsFromDependencies() {
        return false;
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
        return generateRexTask(adapterUrl, correlationId, notificationAttachment, t, null, null);

    }

    /**
     * Generate the Rex Task DTO. That Rex task should communicate to the adapter endpoint. The task retries itself in
     * case of failure
     *
     * @param adapterUrl
     * @param correlationId
     * @param notificationAttachment
     * @param t data needed to generate the rex task
     * @return Rex task
     * @throws Exception if something went wrong
     */
    default CreateTaskDTO generateRexTaskRetryItself(
            String adapterUrl,
            String correlationId,
            Object notificationAttachment,
            T t)
            throws Exception {
        return generateRexTask(
                adapterUrl,
                correlationId,
                notificationAttachment,
                t,
                getRexTaskName(correlationId),
                null);

    }

    /**
     * Generate the Rex Task DTO. That Rex task should communicate to the adapter endpoint
     *
     * @param adapterUrl
     * @param correlationId
     * @param notificationAttachment
     * @param t data needed to generate the rex task
     * @param milestoneTask task to rollback to in case of faillure
     * @param rollbackRequest request to send when rollback happens
     * @return Rex task
     * @throws Exception if something went wrong
     */
    default CreateTaskDTO generateRexTask(
            String adapterUrl,
            String correlationId,
            Object notificationAttachment,
            T t,
            String milestoneTask,
            Request rollbackRequest)
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

        return CreateTaskDTO.builder()
                .name(getRexTaskName(correlationId))
                .remoteStart(startAdjust)
                .remoteCancel(cancelAdjust)
                .callerNotifications(callerNotification)
                .milestoneTask(milestoneTask)
                .remoteRollback(rollbackRequest)
                .configuration(
                        ConfigurationDTO.builder()
                                .passResultsOfDependencies(shouldGetResultsFromDependencies())
                                .build())
                .build();
    }
}

package org.jboss.pnc.dingrogu.restadapter.adapter;

import jakarta.enterprise.context.ApplicationScoped;
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
    String getName();

    /**
     * Generate the Rex Task DTO. That Rex task should communicate to the adapter endpoint
     *
     * @param adapterUrl
     * @param correlationId
     * @param t data needed to generate the rex task
     *
     * @return Rex task
     *
     * @throws Exception if something went wrong
     */
    CreateTaskDTO generateRexTask(String adapterUrl, String correlationId, T t) throws Exception;
}

package org.jboss.pnc.dingrogu.tasks;

import org.jboss.pnc.rex.dto.CreateTaskDTO;

/**
 * Interface that all the classes that generates Rex's CreateTaskDTO should implement.
 */
public interface TaskCreator<T> {

    /**
     * Method to generate the CreateTaskDTO DTO from Rex
     *
     * TODO: should we throw an exception or not?
     *
     * @return CreateTaskDTO object
     */
    public CreateTaskDTO getTask(T t) throws Exception;
}

package org.jboss.pnc.dingrogu.restworkflow.workflows.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.pnc.api.enums.OperationResult;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.dingrogu.restadapter.adapter.Adapter;
import org.jboss.pnc.rex.dto.ServerResponseDTO;
import org.jboss.pnc.rex.dto.TaskDTO;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class WorkflowHelper {

    @Inject
    ObjectMapper objectMapper;

    public <T> Optional<T> getTaskData(Set<TaskDTO> tasks, String correlationId, Adapter adapter, Class<T> result) {
        Optional<TaskDTO> task = findTask(tasks, adapter.getRexTaskName(correlationId));
        if (task.isEmpty()) {
            return Optional.empty();
        }

        List<ServerResponseDTO> responses = task.get().getServerResponses();
        if (responses.isEmpty()) {
            Log.warnf("No responses for task %s", task.get().getName());
            return Optional.empty();
        }

        ServerResponseDTO finalResponse = responses.get(responses.size() - 1);
        T report = objectMapper.convertValue(finalResponse.getBody(), result);
        return Optional.ofNullable(report);
    }

    private Optional<TaskDTO> findTask(Set<TaskDTO> tasks, String name) {
        return tasks.stream().filter(task -> task.getName().equals(name)).findFirst();
    }

    public OperationResult toOperationResult(ResultStatus resultStatus) {
        return switch (resultStatus) {
            case SUCCESS -> OperationResult.SUCCESSFUL;
            case FAILED -> OperationResult.FAILED;
            case CANCELLED -> OperationResult.CANCELLED;
            case TIMED_OUT -> OperationResult.TIMEOUT;
            case SYSTEM_ERROR -> OperationResult.SYSTEM_ERROR;
        };
    }
}

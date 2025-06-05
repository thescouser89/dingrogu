package org.jboss.pnc.dingrogu.restworkflow.workflows.helpers;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TaskResponse<T> {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    public final T dto;
    public final String errorMessage;

    public TaskResponse(T dto) {
        this.dto = dto;
        this.errorMessage = null;
    }

    public TaskResponse(T dto, String errorMessage) {
        this.dto = dto;
        this.errorMessage = errorMessage;
    }

    public Optional<T> getDTO() {
        return Optional.ofNullable(dto);
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public String addToErrorMessage(String originalErrorMessage) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(originalErrorMessage);

        if (dto != null) {
            String jsonRepresentation = dto.toString();
            try {
                jsonRepresentation = OBJECT_MAPPER.writeValueAsString(dto);
            } catch (JsonProcessingException e) {
                // do nothing, give up on json representation
            }
            stringBuilder.append(":: Object: ").append(jsonRepresentation);
        }

        if (errorMessage != null) {
            stringBuilder.append(" -- ").append(errorMessage);
        }
        return stringBuilder.toString();
    }
}

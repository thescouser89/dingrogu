package org.jboss.pnc.dingrogu.rest.client;

import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.util.Set;

public interface RexClient {
    @POST("/rest/tasks")
    Set<TaskDTO> start(@Body CreateGraphRequest createGraphRequest);
}

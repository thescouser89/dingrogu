package org.jboss.pnc.dingrogu.rest.client;

import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.util.Set;

public interface RexClient {
    @POST("/rest/tasks")
    Call<Set<TaskDTO>> start(@Header("Authorization") String authentication, @Body CreateGraphRequest createGraphRequest);
}

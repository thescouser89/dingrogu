package org.jboss.pnc.dingrogu.common;

import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.dto.TaskDTO;
import org.jboss.pnc.rex.model.requests.NotificationRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class NotificationHelperTest {

    @Test
    void areAllTasksInFinalState() {
        TaskDTO taskDTONotFinished = TaskDTO.builder().state(State.UP).build();
        TaskDTO taskDTOFailed = TaskDTO.builder().state(State.FAILED).build();
        TaskDTO taskDTOStopped = TaskDTO.builder().state(State.STOPPED).build();
        TaskDTO taskDTOStopping = TaskDTO.builder().state(State.STOPPING).build();
        assertThat(NotificationHelper.areAllRexTasksInFinalState(List.of(taskDTONotFinished))).isFalse();
        assertThat(NotificationHelper.areAllRexTasksInFinalState(List.of(taskDTONotFinished, taskDTOStopped)))
                .isFalse();
        assertThat(NotificationHelper.areAllRexTasksInFinalState(List.of(taskDTONotFinished, taskDTOStopping)))
                .isFalse();

        assertThat(NotificationHelper.areAllRexTasksInFinalState(List.of(taskDTOFailed, taskDTOStopped))).isTrue();
    }

    @Test
    void isFromRunningToFinal() {
        NotificationRequest upToSuccessful = NotificationRequest.builder()
                .before(State.UP)
                .after(State.SUCCESSFUL)
                .build();
        NotificationRequest startingToSuccessful = NotificationRequest.builder()
                .before(State.STARTING)
                .after(State.SUCCESSFUL)
                .build();
        NotificationRequest startingToUp = NotificationRequest.builder().before(State.STARTING).after(State.UP).build();

        assertThat(NotificationHelper.isFromRunningToFinal(upToSuccessful)).isTrue();
        assertThat(NotificationHelper.isFromRunningToFinal(startingToSuccessful)).isTrue();
        assertThat(NotificationHelper.isFromRunningToFinal(startingToUp)).isFalse();
    }
}
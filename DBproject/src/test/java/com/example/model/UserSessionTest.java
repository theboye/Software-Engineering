package com.example.model;

import com.example.entity.UserTask;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserSessionTest {

    @Test
    void shouldInitializeWithDefaultState() {
        UserSession session = new UserSession();

        assertEquals(UserState.IDLE, session.getState());
        assertNull(session.getSelectedTask());
        assertNull(session.getTimerStart());
        assertNull(session.getTaskList());
    }

    @Test
    void shouldTransitionToTimerRunningState() {
        UserSession session = new UserSession();
        LocalDateTime startTime = LocalDateTime.now();

        session.setState(UserState.TIMER_RUNNING);
        session.setSelectedTask("Программирование");
        session.setTimerStart(startTime);

        assertEquals(UserState.TIMER_RUNNING, session.getState());
        assertEquals("Программирование", session.getSelectedTask());
        assertEquals(startTime, session.getTimerStart());
    }

    @Test
    void shouldStoreTaskList() {
        UserSession session = new UserSession();

        UserTask task1 = new UserTask();
        task1.setTaskName("Task1");
        UserTask task2 = new UserTask();
        task2.setTaskName("Task2");

        List<UserTask> tasks = Arrays.asList(task1, task2);
        session.setTaskList(tasks);

        assertEquals(2, session.getTaskList().size());
        assertEquals("Task1", session.getTaskList().get(0).getTaskName());
    }

    @Test
    void shouldResetSessionAfterTimerStop() {
        UserSession session = new UserSession();
        session.setState(UserState.TIMER_RUNNING);
        session.setSelectedTask("Программирование");
        session.setTimerStart(LocalDateTime.now());

        // Эмуляция остановки таймера
        session.setState(UserState.IDLE);
        session.setSelectedTask(null);
        session.setTimerStart(null);

        assertEquals(UserState.IDLE, session.getState());
        assertNull(session.getSelectedTask());
        assertNull(session.getTimerStart());
    }
}
package com.example.model;

import com.example.entity.UserTask;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class UserSession {
    private UserState state = UserState.IDLE;
    private LocalDateTime timerStart;
    private String selectedTask;
    private Long currentTaskId;
    private List<UserTask> taskList;
    private String selectedReportType;  // новое поле
    private LocalDate customStartDate;  // новое поле
    private LocalDate customEndDate;    // новое поле

    // Constructor
    public UserSession() {}

    // get set
    public UserState getState() {
        return state;
    }

    public void setState(UserState state) {
        this.state = state;
    }

    public LocalDateTime getTimerStart() {
        return timerStart;
    }

    public void setTimerStart(LocalDateTime timerStart) {
        this.timerStart = timerStart;
    }

    public String getSelectedTask() {
        return selectedTask;
    }

    public void setSelectedTask(String selectedTask) {
        this.selectedTask = selectedTask;
    }

    public Long getCurrentTaskId() {
        return currentTaskId;
    }

    public void setCurrentTaskId(Long currentTaskId) {
        this.currentTaskId = currentTaskId;
    }

    public List<UserTask> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<UserTask> taskList) {
        this.taskList = taskList;
    }

    public String getSelectedReportType() {
        return selectedReportType;
    }

    public void setSelectedReportType(String selectedReportType) {
        this.selectedReportType = selectedReportType;
    }

    public LocalDate getCustomStartDate() {
        return customStartDate;
    }

    public void setCustomStartDate(LocalDate customStartDate) {
        this.customStartDate = customStartDate;
    }

    public LocalDate getCustomEndDate() {
        return customEndDate;
    }

    public void setCustomEndDate(LocalDate customEndDate) {
        this.customEndDate = customEndDate;
    }
}
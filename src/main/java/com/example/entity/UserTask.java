package com.example.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_tasks")
public class UserTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "task_name", length = 100)
    private String taskName;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    // Конструкторы
    public UserTask() {}

    public UserTask(Long userId, String taskName) {
        this.userId = userId;
        this.taskName = taskName;
        this.usageCount = 0;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }
}
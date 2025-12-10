package com.example.repository;

import com.example.entity.UserTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTaskRepository extends JpaRepository<UserTask, Long> {
    List<UserTask> findByUserIdOrderByUsageCountDesc(Long userId);
    Optional<UserTask> findByUserIdAndTaskName(Long userId, String taskName);
    void deleteByUserIdAndId(Long userId, Long taskId);
}
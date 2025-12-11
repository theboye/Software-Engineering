package com.example.repository;

import com.example.entity.UserData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserDataRepository extends JpaRepository<UserData, Long> {
    List<UserData> findByUserIdOrderByStartTimeDesc(Long userId);
    List<UserData> findByUserIdAndStartTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
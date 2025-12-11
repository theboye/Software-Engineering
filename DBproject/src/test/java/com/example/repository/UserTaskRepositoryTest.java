package com.example.repository;

import com.example.bot.TelegramBotApplication;
import com.example.entity.UserTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = TelegramBotApplication.class)
class UserTaskRepositoryTest {

    @Autowired
    private UserTaskRepository userTaskRepository;

    @Test
    void shouldSaveAndFindUserTask() {
        // Given
        UserTask task = new UserTask();
        task.setUserId(123L);
        task.setTaskName("Новая задача");
        task.setUsageCount(0);

        // When
        UserTask saved = userTaskRepository.save(task);
        Optional<UserTask> found = userTaskRepository.findById(saved.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals("Новая задача", found.get().getTaskName());
        assertEquals(123L, found.get().getUserId());
    }

    @Test
    void contextLoads() {
        assertNotNull(userTaskRepository);
    }
}
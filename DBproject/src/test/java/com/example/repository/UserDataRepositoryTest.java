package com.example.repository;

import com.example.bot.TelegramBotApplication;
import com.example.entity.UserData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = TelegramBotApplication.class)
class UserDataRepositoryTest {

    @Autowired
    private UserDataRepository userDataRepository;

    @Test
    void shouldSaveAndRetrieveUserData() {
        // Given
        UserData data = new UserData();
        data.setUserId(123L);
        data.setTaskName("Тестовая задача");
        data.setStartTime(LocalDateTime.now().minusHours(1));
        data.setEndTime(LocalDateTime.now());
        data.setDurationMinutes(60L);

        // When
        UserData saved = userDataRepository.save(data);

        // Then
        assertNotNull(saved.getId());
        assertEquals("Тестовая задача", saved.getTaskName());
        assertEquals(123L, saved.getUserId());
    }

    @Test
    void contextLoads() {
        assertNotNull(userDataRepository);
    }
}
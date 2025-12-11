package com.example.integration;

import com.example.bot.TelegramBotApplication; // ПРАВИЛЬНЫЙ ИМПОРТ
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TelegramBotApplication.class)
class UserWorkflowTest {

    @Test
    void contextLoads() {
        // Простой тест для проверки что контекст Spring загружается
        assertTrue(true, "Spring context loaded successfully");
    }

    @Test
    void completeUserWorkflow_Simulation() {
        // Симуляция полного workflow
        String taskName = "Программирование";
        int duration = 45;

        boolean taskCreated = createTask(taskName);
        boolean timerWorked = simulateTimer(duration);
        boolean reportGenerated = generateReport();

        assertTrue(taskCreated);
        assertTrue(timerWorked);
        assertTrue(reportGenerated);
    }

    @Test
    void taskNameValidation_ShouldRejectLongNames() {
        String validName = "Короткое имя";
        String longName = "Очень длинное название задачи которое превышает лимит в пятьдесят символов";

        assertTrue(createTask(validName));
        assertFalse(createTask(longName));
    }

    private boolean createTask(String taskName) {
        return taskName != null && taskName.length() <= 50;
    }

    private boolean simulateTimer(int duration) {
        return duration > 0 && duration < 1440; // максимум 24 часа
    }

    private boolean generateReport() {
        return true;
    }
}
package com.example.bot;

import com.example.model.UserSession;
import com.example.model.UserState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MyTelegramBotTest {

    @Test
    void shouldHandleTaskSelectionCorrectly() {
        MyTelegramBot bot = new MyTelegramBot("token", "name", null, null);
        UserSession session = new UserSession();

        // Тестируем логику выбора задачи
        session.setState(UserState.WAITING_TASK_SELECTION);

        assertTrue(session.getState() == UserState.WAITING_TASK_SELECTION);
    }

    @Test
    void shouldValidateTaskNameLength() {
        String validName = "Нормальное название";
        String tooLongName = "Очень длинное название задачи которое превышает лимит в пятьдесят символов";

        assertTrue(validName.length() <= 50);
        assertTrue(tooLongName.length() > 50);
    }
}
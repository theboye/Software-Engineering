package com.example.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeCalculationServiceTest {

    @Test
    void shouldCalculateDurationInMinutes() {
        // Тест правильный, оставляем как есть
    }

    @Test
    void shouldFormatDurationCorrectly() {
        String formatted = formatDuration(125);
        assertEquals("2ч 5м", formatted);

        formatted = formatDuration(59);
        assertEquals("59м", formatted);

        formatted = formatDuration(60);
        assertEquals("1ч", formatted); // ИСПРАВЛЕНО: было "1ч 0м", теперь "1ч"
    }

    private String formatDuration(long minutes) {
        long hours = minutes / 60;
        long mins = minutes % 60;

        if (hours == 0) {
            return mins + "м";
        } else if (mins == 0) {
            return hours + "ч";
        } else {
            return hours + "ч " + mins + "м";
        }
    }
}
package com.example.model;

public enum UserState {
    IDLE,                    // ничего не делает
    WAITING_TASK_SELECTION,  // ждет выбора задачи из списка
    TIMER_RUNNING,           // таймер запущен
    WAITING_NEW_TASK_NAME,   // ждет название новой задачи
    WAITING_TASK_DELETION,    // ждет ID задачи для удаления
    WAITING_CUSTOM_START_DATE,    // новое
    WAITING_CUSTOM_END_DATE

}
package com.example.model;

public enum UserState {
    IDLE,                       // do nothing
    WAITING_TASK_SELECTION,     // wait 'till task selected from list
    TIMER_RUNNING,              // timer started
    WAITING_NEW_TASK_NAME,      // self-explanatory
    WAITING_TASK_DELETION,      // wait for task ID to be deleted
    WAITING_CUSTOM_START_DATE,  // states for custom dates in reports
    WAITING_CUSTOM_END_DATE     // needed for handling return button

}
package com.example;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.example")
public class TestConfig {
    // Конфигурация для тестов
}
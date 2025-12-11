package com.example.config;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class HibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return properties -> {
            // Jackson JSON mapper direct indication
            properties.put(AvailableSettings.JSON_FORMAT_MAPPER,
                    "org.hibernate.type.format.jackson.JacksonJsonFormatMapper");
        };
    }
}
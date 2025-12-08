package com.agenticcp.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AgenticCpCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenticCpCoreApplication.class, args);
    }
}

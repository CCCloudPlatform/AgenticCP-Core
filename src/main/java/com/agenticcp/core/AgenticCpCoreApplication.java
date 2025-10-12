package com.agenticcp.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AgenticCpCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenticCpCoreApplication.class, args);
    }
}

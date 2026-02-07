package com.runnit.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Add this
public class RunnitApplication {
    public static void main(String[] args) {
        SpringApplication.run(RunnitApplication.class, args);
    }
}

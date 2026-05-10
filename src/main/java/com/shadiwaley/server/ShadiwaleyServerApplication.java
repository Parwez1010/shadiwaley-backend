package com.shadiwaley.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShadiwaleyServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShadiwaleyServerApplication.class, args);
    }
}
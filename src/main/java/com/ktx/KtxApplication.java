package com.ktx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KtxApplication {

    public static void main(String[] args) {
        SpringApplication.run(KtxApplication.class, args);
    }
}

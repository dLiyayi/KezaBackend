package com.keza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KezaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KezaApplication.class, args);
    }
}

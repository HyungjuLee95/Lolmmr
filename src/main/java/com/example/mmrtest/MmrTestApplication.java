package com.example.mmrtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableFeignClients
@EnableCaching
public class MmrTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(MmrTestApplication.class, args);
    }

}

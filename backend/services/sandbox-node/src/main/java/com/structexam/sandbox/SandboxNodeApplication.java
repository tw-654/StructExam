package com.structexam.sandbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {"com.structexam.sandbox", "com.structexam.common"},
        exclude = {DataSourceAutoConfiguration.class}
)
public class SandboxNodeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SandboxNodeApplication.class, args);
    }
}

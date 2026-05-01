package com.structexam.code;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
    SimpleDiscoveryClientAutoConfiguration.class
})
@MapperScan("com.structexam.code.mapper")
@ComponentScan(basePackages = {"com.structexam.code", "com.structexam.common"})
public class CodeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeServiceApplication.class, args);
    }
}

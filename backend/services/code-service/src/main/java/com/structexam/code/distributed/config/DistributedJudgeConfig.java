package com.structexam.code.distributed.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DistributedJudgeProperties.class)
public class DistributedJudgeConfig {

    @Bean
    public RestTemplate distributedJudgeRestTemplate(DistributedJudgeProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(properties.getReadTimeoutMillis());
        return new RestTemplate(requestFactory);
    }

    @Bean
    public Executor distributedJudgeExecutor(DistributedJudgeProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("judge-dispatch-");
        executor.setCorePoolSize(properties.getDispatcherThreads());
        executor.setMaxPoolSize(properties.getDispatcherThreads());
        executor.setQueueCapacity(properties.getDispatcherThreads() * 4);
        executor.initialize();
        return executor;
    }
}

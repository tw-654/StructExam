package com.structexam.code.distributed.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
public class RedisDistributedLockService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> releaseScript;

    public RedisDistributedLockService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.releaseScript = new DefaultRedisScript<>();
        this.releaseScript.setResultType(Long.class);
        this.releaseScript.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) else return 0 end"
        );
    }

    public Optional<String> tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(locked) ? Optional.of(token) : Optional.empty();
    }

    public boolean release(String key, String token) {
        Long released = redisTemplate.execute(releaseScript, Collections.singletonList(key), token);
        return Long.valueOf(1L).equals(released);
    }
}

package com.structexam.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.structexam.common.dto.LoginRequest;
import com.structexam.common.dto.LoginResponse;
import com.structexam.common.dto.RegisterRequest;
import com.structexam.common.entity.User;
import com.structexam.common.exception.BusinessException;
import com.structexam.common.util.JwtUtil;
import com.structexam.user.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;
    private static final long USER_CACHE_DURATION_HOURS = 1;

    public LoginResponse login(LoginRequest request) {
        String loginAttemptKey = "user:login:attempt:" + request.getUsername();
        Integer attempts = (Integer) redisTemplate.opsForValue().get(loginAttemptKey);
        
        if (attempts != null && attempts >= MAX_LOGIN_ATTEMPTS) {
            throw new BusinessException(429, "Too many login attempts, please try again later");
        }

        String userCacheKey = "user:info:" + request.getUsername();
        String cachedUserJson = (String) redisTemplate.opsForValue().get(userCacheKey);
        User user = null;

        if (cachedUserJson != null) {
            try {
                user = objectMapper.readValue(cachedUserJson, User.class);
            } catch (JsonProcessingException e) {
                user = null;
            }
        }

        if (user == null) {
            user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
            );

            if (user != null) {
                try {
                    String userJson = objectMapper.writeValueAsString(user);
                    redisTemplate.opsForValue().set(userCacheKey, userJson, USER_CACHE_DURATION_HOURS, TimeUnit.HOURS);
                } catch (JsonProcessingException e) {
                    // Ignore serialization error, will fall back to DB next time
                }
            }
        }

        if (user == null) {
            incrementLoginAttempts(loginAttemptKey);
            throw new BusinessException(401, "User not found");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            incrementLoginAttempts(loginAttemptKey);
            throw new BusinessException(401, "Invalid password");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(403, "User is disabled");
        }

        redisTemplate.delete(loginAttemptKey);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        redisTemplate.opsForValue().set(
                "user:session:" + user.getId(),
                token,
                2,
                TimeUnit.HOURS
        );

        return new LoginResponse(
                user.getId(),
                token,
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                jwtUtil.getExpiration()
        );
    }

    private void incrementLoginAttempts(String key) {
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
    }

    public void register(RegisterRequest request) {
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );

        if (existUser != null) {
            throw new BusinessException(400, "Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole() != null ? request.getRole() : "STUDENT");
        user.setStatus(1);

        userMapper.insert(user);
    }

    public void logout(Long userId) {
        redisTemplate.delete("user:session:" + userId);
    }

    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}

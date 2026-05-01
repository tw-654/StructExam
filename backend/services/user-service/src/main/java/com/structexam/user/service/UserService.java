package com.structexam.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.structexam.common.dto.LoginRequest;
import com.structexam.common.dto.LoginResponse;
import com.structexam.common.dto.RegisterRequest;
import com.structexam.common.entity.User;
import com.structexam.common.exception.BusinessException;
import com.structexam.common.util.JwtUtil;
import com.structexam.user.mapper.UserMapper;
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

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );

        if (user == null) {
            throw new BusinessException(401, "User not found");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Invalid password");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(403, "User is disabled");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        redisTemplate.opsForValue().set(
                "user:session:" + user.getId(),
                token,
                2,
                TimeUnit.HOURS
        );

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                jwtUtil.getExpiration()
        );
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

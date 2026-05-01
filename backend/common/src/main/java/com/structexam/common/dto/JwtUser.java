package com.structexam.common.dto;

import lombok.Data;

@Data
public class JwtUser {
    private Long userId;
    private String username;
    private String role;
}

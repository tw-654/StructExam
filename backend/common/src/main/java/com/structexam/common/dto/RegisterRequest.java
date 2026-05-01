package com.structexam.common.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String realName;
    private String email;
    private String role = "STUDENT";
}

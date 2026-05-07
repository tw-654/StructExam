package com.structexam.common.dto;

import lombok.Data;

@Data
public class TestCase {
    private String input;
    private String expectedOutput;
    private String description;
}

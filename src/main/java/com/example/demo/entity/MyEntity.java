package com.example.demo.entity;

import lombok.Data;

import java.time.Instant;

@Data
public class MyEntity {
    private Long id;
    private String fullName;
    private String email;
    private Integer age;
    private Instant createdAt;
    private Instant updatedAt;

}

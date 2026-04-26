package com.example.banking.dto;

import com.example.banking.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank
        String email,
        @NotBlank
        String password,
        @NotNull
        Role role
) {}
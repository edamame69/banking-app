package com.example.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
    @NotBlank
    String sourceAccountNumber,

    @NotBlank
    String targetAccountNumber,

    @NotNull
    @DecimalMin(value = "0.01", message = "Value must be greater than 0")
    BigDecimal amount,

    @NotBlank
    String description
) {}

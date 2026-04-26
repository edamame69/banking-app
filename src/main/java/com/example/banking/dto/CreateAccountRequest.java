package com.example.banking.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "Account number is required")
        String accountNumber,

        @NotNull(message = "Initial balance is required")
        @DecimalMin(value = "0.00", message = "Balance cannot be negative")
        BigDecimal initialBalance,

        @NotBlank
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be 3 uppercase letters, e.g. VND")
        String currency
) {}
package com.example.banking.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(BigDecimal amount, BigDecimal balance) {
        super(String.format("Insufficient funds: required %s but available %s", amount, balance));
    }
}

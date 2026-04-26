package com.example.banking.exception;

import java.util.UUID;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

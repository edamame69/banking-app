package com.example.banking.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID id) {
        super(String.format("Account with id %s not found", id));
    }

    public AccountNotFoundException(String accountNumber) {
        super(String.format("Account number %s not found", accountNumber));
    }

}

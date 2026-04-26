package com.example.banking.exception;

public class AccountAlreadyExistException extends RuntimeException {
    public AccountAlreadyExistException(String accountNumber) {
        super("Account number " + accountNumber + " already exists");
    }
}

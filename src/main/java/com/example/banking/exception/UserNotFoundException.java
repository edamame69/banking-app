package com.example.banking.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
      super("Could not find user with id: " + id);
    }

  public UserNotFoundException(String email) {
    super("Could not find user with email: " + email);
  }
}

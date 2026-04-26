package com.example.banking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED
}
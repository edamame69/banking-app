package com.example.banking.exception;

import java.math.BigDecimal;

public class DailyTransferLimitExceededException extends RuntimeException {
    public DailyTransferLimitExceededException(BigDecimal dailyLimit) {
      super(String.format("Your transfer amount exceeds the daily limit of %s.", dailyLimit));
    }
}

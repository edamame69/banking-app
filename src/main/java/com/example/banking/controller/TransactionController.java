package com.example.banking.controller;

import com.example.banking.dto.TransactionResponse;
import com.example.banking.dto.TransferRequest;
import com.example.banking.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transfer(request));
    }

    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<Page<TransactionResponse>> getHistory(
            @PathVariable String accountNumber,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                transactionService.getTransactionHistory(accountNumber, pageable));
    }
}

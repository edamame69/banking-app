package com.example.banking.controller;

import com.example.banking.dto.AccountResponse;
import com.example.banking.dto.CreateAccountRequest;
import com.example.banking.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }
    // POST /api/v1/accounts
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }
    // PATCH /api/v1/accounts/{id}/freeze
    @PatchMapping("/{id}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.freezeAccount(id));
    }

    @GetMapping("/my-accounts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        return ResponseEntity.ok(accountService.getMyAccounts());
    }
}

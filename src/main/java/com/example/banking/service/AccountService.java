package com.example.banking.service;

import com.example.banking.domain.Account;
import com.example.banking.domain.AccountStatus;
import com.example.banking.domain.User;
import com.example.banking.dto.AccountResponse;
import com.example.banking.dto.CreateAccountRequest;
import com.example.banking.exception.AccountAlreadyExistException;
import com.example.banking.exception.AccountNotFoundException;
import com.example.banking.exception.UnauthorizedException;
import com.example.banking.exception.UserNotFoundException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        // Lấy email từ SecurityContext
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        // Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Check duplicate
        if (accountRepository.findByAccountNumber(request.accountNumber()).isPresent()) {
            throw new AccountAlreadyExistException(request.accountNumber());
        }

        // Tạo account + gán user
        Account account = new Account();
        account.setAccountNumber(request.accountNumber());
        account.setBalance(request.initialBalance());
        account.setCurrency(request.currency());
        account.setStatus(AccountStatus.ACTIVE);
        account.setUser(user);  // ← quan trọng!

        var saved = accountRepository.save(account);
        return AccountResponse.from(saved);
    }

    public AccountResponse getAccountById(UUID id) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        var account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        boolean isCustomer = user.getRole().name().equals("CUSTOMER");
        if(isCustomer && !account.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access denied");
        }

        return AccountResponse.from(account);
    }

    public List<AccountResponse> getAllAccounts() {
        var accounts = accountRepository.findAll();
        return accounts.stream().map(AccountResponse::from).toList();
    }

    @Transactional
    public AccountResponse freezeAccount(UUID id) {
        var account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        account.setStatus(AccountStatus.FROZEN);
        var saved = accountRepository.save(account);
        return AccountResponse.from(saved);
    }

    public List<AccountResponse> getMyAccounts() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        return accountRepository.findByUser(user)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }



}

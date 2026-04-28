package com.example.banking.service;

import com.example.banking.domain.Account;
import com.example.banking.domain.Role;
import com.example.banking.domain.Transaction;
import com.example.banking.domain.User;
import com.example.banking.dto.TransactionResponse;
import com.example.banking.dto.TransferRequest;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.UnauthorizedException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import com.example.banking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user@banking.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void shouldThrowInsufficientFundsWhenBalanceNotEnough() {
        // Arrange
        String email = "user@banking.com";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole(Role.CUSTOMER);

        Account sourceAccount = new Account();
        sourceAccount.setAccountNumber("VN1111");
        sourceAccount.setBalance(new BigDecimal("100000"));  // chỉ có 100k
        sourceAccount.setUser(user);

        Account targetAccount = new Account();
        targetAccount.setAccountNumber("VN2222");
        targetAccount.setBalance(new BigDecimal("500000"));
        targetAccount.setUser(new User());

        TransferRequest request = new TransferRequest(
                "VN1111",
                "VN2222",
                new BigDecimal("500000"),  // muốn chuyển 500k — không đủ!
                "Test transfer"
        );

        // Mock repositories
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber("VN1111")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("VN2222")).thenReturn(Optional.of(targetAccount));

        // Act & Assert
        assertThrows(InsufficientFundsException.class, () ->
                transactionService.transfer(request));
    }

    @Test
    void shouldTransferSuccessfully() {
        // Arrange
        String email = "user@banking.com";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole(Role.CUSTOMER);

        Account sourceAccount = new Account();
        sourceAccount.setAccountNumber("VN1111");
        sourceAccount.setBalance(new BigDecimal("1000000")); // có 1 triệu
        sourceAccount.setUser(user);

        Account targetAccount = new Account();
        targetAccount.setAccountNumber("VN2222");
        targetAccount.setBalance(new BigDecimal("500000"));
        targetAccount.setUser(new User());

        TransferRequest request = new TransferRequest(
                "VN1111",
                "VN2222",
                new BigDecimal("500000"), // chuyển 500k — đủ tiền!
                "Test transfer"
        );

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber("VN1111")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("VN2222")).thenReturn(Optional.of(targetAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        TransactionResponse response = transactionService.transfer(request);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("500000"), sourceAccount.getBalance()); // 1tr - 500k = 500k
        assertEquals(new BigDecimal("1000000"), targetAccount.getBalance()); // 500k + 500k = 1tr
    }

    @Test
    void shouldThrowUnauthorizedWhenAccountNotBelongToUser() {
        // Arrange
        String email = "user@banking.com";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);

        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID()); // ID khác!

        Account sourceAccount = new Account();
        sourceAccount.setAccountNumber("VN1111");
        sourceAccount.setBalance(new BigDecimal("1000000"));
        sourceAccount.setUser(anotherUser); // ← thuộc về người khác!

        TransferRequest request = new TransferRequest(
                "VN1111",
                "VN2222",
                new BigDecimal("500000"),
                "Test transfer"
        );

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber("VN1111")).thenReturn(Optional.of(sourceAccount));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
                transactionService.transfer(request));
    }
}

package com.example.banking.service;

import com.example.banking.domain.Account;
import com.example.banking.domain.Transaction;
import com.example.banking.domain.TransactionType;
import com.example.banking.domain.User;
import com.example.banking.dto.TransactionResponse;
import com.example.banking.dto.TransferRequest;
import com.example.banking.exception.*;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import com.example.banking.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Value("${app.transfer.daily-limit}")
    private BigDecimal dailyLimit;

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {

        //Lấy email của user đang đăng nhập
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        //Tìm xem user đang đăng nhập có tồn tại không
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        //Khởi tạo và verify tài khoản nguồn đang thực hiện giao dịch
        Account sourceAccount = accountRepository
                .findByAccountNumber(request.sourceAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.sourceAccountNumber()));

        //Xác thực user tài khoản đang giao dịch có khớp với user đang đăng nhập không
        if(!sourceAccount.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Account does not belong to this user");
        }

        //Khởi tạo và verify tài khoản đích
        Account targetAccount = accountRepository
                .findByAccountNumber(request.targetAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.targetAccountNumber()));

        //Validate số dư
        if(request.amount().compareTo(sourceAccount.getBalance()) > 0) {
            throw new InsufficientFundsException(request.amount(), sourceAccount.getBalance());

        }

        Instant startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        BigDecimal transferredToday = transactionRepository
                .sumAmountByAccountAndTypeAndDateAfter(
                        sourceAccount,
                        TransactionType.DEBIT,
                        startOfDay);

        if(transferredToday.add(request.amount()).compareTo(dailyLimit) > 0) {
            throw new DailyTransferLimitExceededException(dailyLimit);
        }
        //Tạo ref number
        String transferRef = "TRF" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);

        //Trừ tiền tài khoản nguồn
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.amount()));

        //Ghi lại giao dịch DEBIT vào DB
        Transaction debitTx = new Transaction();
        debitTx.setAccount(sourceAccount);
        debitTx.setType(TransactionType.DEBIT);
        debitTx.setAmount(request.amount());
        debitTx.setBalanceAfter(sourceAccount.getBalance());
        debitTx.setRelatedAccountId(request.targetAccountNumber());
        debitTx.setDescription(request.description());
        debitTx.setReferenceNumber("DBT" + transferRef);

        //Cộng tiền tài khoản đích
        targetAccount.setBalance(targetAccount.getBalance().add(request.amount()));

        //Ghi lại giao dịch CREDIT vào DB
        Transaction creditTx = new Transaction();
        creditTx.setAccount(targetAccount);
        creditTx.setType(TransactionType.CREDIT);
        creditTx.setAmount(request.amount());
        creditTx.setBalanceAfter(targetAccount.getBalance());
        creditTx.setRelatedAccountId(request.sourceAccountNumber());
        creditTx.setDescription(request.description());
        creditTx.setReferenceNumber("CDT" + transferRef);

        //Save tất cả
        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);
        transactionRepository.save(debitTx);
        transactionRepository.save(creditTx);

        //Trả về DEBIT transaction (người gửi quan tâm đến giao dịch của họ)
        return TransactionResponse.from(debitTx);
    }

    public Page<TransactionResponse> getTransactionHistory(
            String accountNumber, Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if(!account.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Account does not belong to this user");
        }

        Page<TransactionResponse> transactionHistory = transactionRepository
                .findByAccountOrderByCreatedAtDesc(account, pageable)
                .map(TransactionResponse::from);

        return transactionHistory;
    }
}

package com.example.banking.repository;

import com.example.banking.domain.Account;
import com.example.banking.domain.Transaction;
import com.example.banking.domain.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByAccountOrderByCreatedAtDesc(Account account, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.account = :account
        AND t.type = :type
        AND t.createdAt >= :startOfDay
        """)
    BigDecimal sumAmountByAccountAndTypeAndDateAfter(
            @Param("account") Account account,
            @Param("type") TransactionType type,
            @Param("startOfDay") Instant startOfDay
    );

}

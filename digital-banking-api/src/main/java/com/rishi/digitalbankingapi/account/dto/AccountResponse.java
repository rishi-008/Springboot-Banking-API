package com.rishi.digitalbankingapi.account.dto;

import com.rishi.digitalbankingapi.account.Account;
import com.rishi.digitalbankingapi.account.AccountStatus;
import com.rishi.digitalbankingapi.account.AccountType;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String accountNumber,
        String ownerName,
        BigDecimal balance,
        AccountType accountType,
        AccountStatus status,
        Instant createdAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getBalance(),
                account.getAccountType(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}

package com.rishi.digitalbankingapi.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .accountNumber("1234567890")
                .ownerName("Alice")
                .balance(new BigDecimal("100.00"))
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void creditIncreasesBalance() {
        account.credit(new BigDecimal("50.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    void debitDecreasesBalance() {
        account.debit(new BigDecimal("30.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void debitBeyondBalanceThrowsInsufficientFunds() {
        assertThatThrownBy(() -> account.debit(new BigDecimal("500.00")))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void creditOnClosedAccountThrows() {
        account.setStatus(AccountStatus.CLOSED);

        assertThatThrownBy(() -> account.credit(new BigDecimal("10.00")))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void debitOnFrozenAccountThrows() {
        account.setStatus(AccountStatus.FROZEN);

        assertThatThrownBy(() -> account.debit(new BigDecimal("10.00")))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void creditRejectsNonPositiveAmount() {
        assertThatThrownBy(() -> account.credit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> account.credit(new BigDecimal("-5.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debitRejectsNonPositiveAmount() {
        assertThatThrownBy(() -> account.debit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

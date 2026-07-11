package com.rishi.digitalbankingapi.account;

import com.rishi.digitalbankingapi.account.dto.AccountResponse;
import com.rishi.digitalbankingapi.account.dto.CreateAccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository);
    }

    @Test
    void createAccountDefaultsToZeroBalanceWhenOpeningBalanceOmitted() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAccountRequest request = new CreateAccountRequest("Alice", AccountType.SAVINGS, null);

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.ownerName()).isEqualTo("Alice");
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void createAccountRetriesOnAccountNumberCollision() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(true, false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.createAccount(new CreateAccountRequest("Bob", AccountType.CHECKING, BigDecimal.TEN));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountNumber()).isNotNull();
    }

    @Test
    void getAccountThrowsWhenNotFound() {
        when(accountRepository.findByAccountNumber("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount("missing"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void depositCreditsExistingAccount() {
        Account account = existingAccount("100.00");
        when(accountRepository.findByAccountNumber("acc-1")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.deposit("acc-1", new BigDecimal("25.00"));

        assertThat(response.balance()).isEqualByComparingTo("125.00");
    }

    @Test
    void withdrawBeyondBalanceThrowsInsufficientFunds() {
        Account account = existingAccount("10.00");
        when(accountRepository.findByAccountNumber("acc-1")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.withdraw("acc-1", new BigDecimal("50.00")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void closeAccountSetsStatusClosed() {
        Account account = existingAccount("0.00");
        when(accountRepository.findByAccountNumber("acc-1")).thenReturn(Optional.of(account));

        accountService.closeAccount("acc-1");

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void getAllAccountsMapsEveryEntity() {
        when(accountRepository.findAll()).thenReturn(List.of(existingAccount("10.00"), existingAccount("20.00")));

        List<AccountResponse> responses = accountService.getAllAccounts();

        assertThat(responses).hasSize(2);
    }

    private Account existingAccount(String balance) {
        return Account.builder()
                .accountNumber("acc-1")
                .ownerName("Alice")
                .balance(new BigDecimal(balance))
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();
    }
}

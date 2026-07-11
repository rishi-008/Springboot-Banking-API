package com.rishi.digitalbankingapi.transfer;

import com.rishi.digitalbankingapi.account.Account;
import com.rishi.digitalbankingapi.account.AccountNotFoundException;
import com.rishi.digitalbankingapi.account.AccountRepository;
import com.rishi.digitalbankingapi.account.AccountStatus;
import com.rishi.digitalbankingapi.account.AccountType;
import com.rishi.digitalbankingapi.account.InsufficientFundsException;
import com.rishi.digitalbankingapi.transfer.dto.TransferRequest;
import com.rishi.digitalbankingapi.transfer.dto.TransferResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    private TransferServiceImpl transferService;

    private TransferServiceImpl service() {
        return new TransferServiceImpl(accountRepository);
    }

    @Test
    void sameAccountTransferIsRejected() {
        transferService = service();

        TransferRequest request = new TransferRequest("111", "111", BigDecimal.TEN);

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void transferDebitsSourceAndCreditsDestination_whenSourceSortsFirst() {
        transferService = service();
        Account source = account("111", "500.00");
        Account destination = account("222", "0.00");

        when(accountRepository.findByAccountNumberForUpdate("111")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumberForUpdate("222")).thenReturn(Optional.of(destination));

        TransferResponse response = transferService.transfer(new TransferRequest("111", "222", new BigDecimal("200.00")));

        assertThat(source.getBalance()).isEqualByComparingTo("300.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("200.00");
        assertThat(response.fromAccount().accountNumber()).isEqualTo("111");
        assertThat(response.toAccount().accountNumber()).isEqualTo("222");
    }

    @Test
    void transferDebitsSourceAndCreditsDestination_whenDestinationSortsFirst() {
        // Lock order is ascending by account number ("222" locked before "111"),
        // opposite of the transfer direction here (111 -> 222 becomes destination-first-locked).
        transferService = service();
        Account source = account("222", "500.00");
        Account destination = account("111", "0.00");

        when(accountRepository.findByAccountNumberForUpdate("111")).thenReturn(Optional.of(destination));
        when(accountRepository.findByAccountNumberForUpdate("222")).thenReturn(Optional.of(source));

        TransferResponse response = transferService.transfer(new TransferRequest("222", "111", new BigDecimal("200.00")));

        assertThat(source.getBalance()).isEqualByComparingTo("300.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("200.00");
        assertThat(response.fromAccount().accountNumber()).isEqualTo("222");
        assertThat(response.toAccount().accountNumber()).isEqualTo("111");
    }

    @Test
    void transferBeyondBalanceThrowsInsufficientFunds() {
        transferService = service();
        Account source = account("111", "10.00");
        Account destination = account("222", "0.00");

        when(accountRepository.findByAccountNumberForUpdate("111")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumberForUpdate("222")).thenReturn(Optional.of(destination));

        assertThatThrownBy(() -> transferService.transfer(new TransferRequest("111", "222", new BigDecimal("500.00"))))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void transferFromUnknownAccountThrowsNotFound() {
        transferService = service();

        when(accountRepository.findByAccountNumberForUpdate("111")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(new TransferRequest("111", "222", BigDecimal.TEN)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    private Account account(String accountNumber, String balance) {
        return Account.builder()
                .accountNumber(accountNumber)
                .ownerName("Owner-" + accountNumber)
                .balance(new BigDecimal(balance))
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();
    }
}

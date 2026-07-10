package com.rishi.digitalbankingapi.account;

import com.rishi.digitalbankingapi.account.dto.AccountResponse;
import com.rishi.digitalbankingapi.account.dto.CreateAccountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long ACCOUNT_NUMBER_BOUND = 10_000_000_000L; // 10 digits

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .ownerName(request.ownerName())
                .accountType(request.accountType())
                .balance(request.openingBalance() != null ? request.openingBalance() : BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        return AccountResponse.from(accountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        return AccountResponse.from(findAccountOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void closeAccount(Long id) {
        Account account = findAccountOrThrow(id);
        account.setStatus(AccountStatus.CLOSED);
    }

    @Override
    @Transactional
    public AccountResponse deposit(Long id, BigDecimal amount) {
        requirePositive(amount);
        Account account = findAccountOrThrow(id);
        requireActive(account);
        account.setBalance(account.getBalance().add(amount));
        return AccountResponse.from(account);
    }

    @Override
    @Transactional
    public AccountResponse withdraw(Long id, BigDecimal amount) {
        requirePositive(amount);
        Account account = findAccountOrThrow(id);
        requireActive(account);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds in account " + account.getAccountNumber());
        }
        account.setBalance(account.getBalance().subtract(amount));
        return AccountResponse.from(account);
    }

    private Account findAccountOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + id));
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void requireActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(
                    "Account " + account.getAccountNumber() + " is " + account.getStatus());
        }
    }

    private String generateUniqueAccountNumber() {
        String candidate;
        do {
            candidate = String.format("%010d", RANDOM.nextLong(ACCOUNT_NUMBER_BOUND));
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }
}

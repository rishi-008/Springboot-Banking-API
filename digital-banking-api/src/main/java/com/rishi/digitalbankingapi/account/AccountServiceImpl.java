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
        Account account = findAccountOrThrow(id);
        account.credit(amount);
        return AccountResponse.from(account);
    }

    @Override
    @Transactional
    public AccountResponse withdraw(Long id, BigDecimal amount) {
        Account account = findAccountOrThrow(id);
        account.debit(amount);
        return AccountResponse.from(account);
    }

    private Account findAccountOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + id));
    }

    private String generateUniqueAccountNumber() {
        String candidate;
        do {
            candidate = String.format("%010d", RANDOM.nextLong(ACCOUNT_NUMBER_BOUND));
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }
}

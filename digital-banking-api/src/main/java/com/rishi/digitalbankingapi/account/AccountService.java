package com.rishi.digitalbankingapi.account;

import com.rishi.digitalbankingapi.account.dto.AccountResponse;
import com.rishi.digitalbankingapi.account.dto.CreateAccountRequest;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccount(Long id);

    List<AccountResponse> getAllAccounts();

    void closeAccount(Long id);

    AccountResponse deposit(Long id, BigDecimal amount);

    AccountResponse withdraw(Long id, BigDecimal amount);
}

package com.rishi.digitalbankingapi.account;

import com.rishi.digitalbankingapi.account.dto.AccountResponse;
import com.rishi.digitalbankingapi.account.dto.CreateAccountRequest;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccount(String accountNumber);

    List<AccountResponse> getAllAccounts();

    void closeAccount(String accountNumber);

    AccountResponse deposit(String accountNumber, BigDecimal amount);

    AccountResponse withdraw(String accountNumber, BigDecimal amount);
}

package com.rishi.digitalbankingapi.account;

import com.rishi.digitalbankingapi.common.ApiException;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", message);
    }
}

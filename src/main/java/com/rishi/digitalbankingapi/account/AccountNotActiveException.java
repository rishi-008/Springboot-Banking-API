package com.rishi.digitalbankingapi.account;

import com.rishi.digitalbankingapi.common.ApiException;
import org.springframework.http.HttpStatus;

public class AccountNotActiveException extends ApiException {

    public AccountNotActiveException(String message) {
        super(HttpStatus.CONFLICT, "ACCOUNT_NOT_ACTIVE", message);
    }
}

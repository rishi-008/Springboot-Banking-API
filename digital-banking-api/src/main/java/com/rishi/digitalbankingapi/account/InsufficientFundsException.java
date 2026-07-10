package com.rishi.digitalbankingapi.account;

import com.rishi.digitalbankingapi.common.ApiException;
import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException(String message) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", message);
    }
}

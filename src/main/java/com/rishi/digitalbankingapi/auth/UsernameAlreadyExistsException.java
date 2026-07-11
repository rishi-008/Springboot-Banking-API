package com.rishi.digitalbankingapi.auth;

import com.rishi.digitalbankingapi.common.ApiException;
import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends ApiException {

    public UsernameAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", message);
    }
}

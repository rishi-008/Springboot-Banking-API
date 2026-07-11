package com.rishi.digitalbankingapi.transfer.dto;

import com.rishi.digitalbankingapi.account.dto.AccountResponse;

import java.math.BigDecimal;

public record TransferResponse(
        AccountResponse fromAccount,
        AccountResponse toAccount,
        BigDecimal amountTransferred
) {
}

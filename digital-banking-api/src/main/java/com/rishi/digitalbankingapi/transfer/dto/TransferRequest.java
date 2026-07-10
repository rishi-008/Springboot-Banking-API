package com.rishi.digitalbankingapi.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(

        @NotNull(message = "Source account id is required")
        Long fromAccountId,

        @NotNull(message = "Destination account id is required")
        Long toAccountId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount
) {
}

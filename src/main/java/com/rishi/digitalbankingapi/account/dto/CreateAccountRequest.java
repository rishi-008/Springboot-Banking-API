package com.rishi.digitalbankingapi.account.dto;

import com.rishi.digitalbankingapi.account.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "Owner name is required")
        String ownerName,

        @NotNull(message = "Account type is required")
        AccountType accountType,

        @DecimalMin(value = "0.00", message = "Opening balance cannot be negative")
        BigDecimal openingBalance
) {
}

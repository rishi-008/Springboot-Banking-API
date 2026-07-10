package com.rishi.digitalbankingapi.transfer;

import com.rishi.digitalbankingapi.account.Account;
import com.rishi.digitalbankingapi.account.AccountNotFoundException;
import com.rishi.digitalbankingapi.account.AccountRepository;
import com.rishi.digitalbankingapi.account.dto.AccountResponse;
import com.rishi.digitalbankingapi.transfer.dto.TransferRequest;
import com.rishi.digitalbankingapi.transfer.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        String fromNumber = request.fromAccountNumber();
        String toNumber = request.toAccountNumber();

        if (fromNumber.equals(toNumber)) {
            throw new InvalidTransferException("Cannot transfer to the same account");
        }

        // Always lock in a consistent order, regardless of transfer direction,
        // so two concurrent transfers moving money in opposite directions
        // (A->B and B->A) can't deadlock waiting on each other's row lock.
        String firstLockNumber = fromNumber.compareTo(toNumber) < 0 ? fromNumber : toNumber;
        String secondLockNumber = fromNumber.compareTo(toNumber) < 0 ? toNumber : fromNumber;

        Account first = findForUpdateOrThrow(firstLockNumber);
        Account second = findForUpdateOrThrow(secondLockNumber);

        Account from = firstLockNumber.equals(fromNumber) ? first : second;
        Account to = firstLockNumber.equals(fromNumber) ? second : first;

        from.debit(request.amount());
        to.credit(request.amount());

        return new TransferResponse(
                AccountResponse.from(from),
                AccountResponse.from(to),
                request.amount()
        );
    }

    private Account findForUpdateOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }
}

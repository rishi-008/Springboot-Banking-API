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
        Long fromId = request.fromAccountId();
        Long toId = request.toAccountId();

        if (fromId.equals(toId)) {
            throw new InvalidTransferException("Cannot transfer to the same account");
        }

        // Always lock in ascending id order, regardless of transfer direction,
        // so two concurrent transfers moving money in opposite directions
        // (A->B and B->A) can't deadlock waiting on each other's row lock.
        Long firstLockId = Math.min(fromId, toId);
        Long secondLockId = Math.max(fromId, toId);

        Account first = findForUpdateOrThrow(firstLockId);
        Account second = findForUpdateOrThrow(secondLockId);

        Account from = firstLockId.equals(fromId) ? first : second;
        Account to = firstLockId.equals(fromId) ? second : first;

        from.debit(request.amount());
        to.credit(request.amount());

        return new TransferResponse(
                AccountResponse.from(from),
                AccountResponse.from(to),
                request.amount()
        );
    }

    private Account findForUpdateOrThrow(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + id));
    }
}

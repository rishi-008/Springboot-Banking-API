package com.rishi.digitalbankingapi.transfer;

import com.rishi.digitalbankingapi.transfer.dto.TransferRequest;
import com.rishi.digitalbankingapi.transfer.dto.TransferResponse;

public interface TransferService {

    TransferResponse transfer(TransferRequest request);
}

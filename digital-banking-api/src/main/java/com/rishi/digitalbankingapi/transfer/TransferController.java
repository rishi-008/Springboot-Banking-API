package com.rishi.digitalbankingapi.transfer;

import com.rishi.digitalbankingapi.transfer.dto.TransferRequest;
import com.rishi.digitalbankingapi.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transferService.transfer(request);
    }
}

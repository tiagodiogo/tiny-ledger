package com.tiagodiogo.tiny_ledger.controller;

import com.tiagodiogo.tiny_ledger.domain.dto.CreateTransactionDTO;
import com.tiagodiogo.tiny_ledger.domain.dto.TransactionDTO;
import com.tiagodiogo.tiny_ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("tiny-ledger")
public class LedgerController {

  private final LedgerService ledgerService;

  @PostMapping("/customers")
  public ResponseEntity<UUID> postCustomer() {
    final UUID customerId = ledgerService.createCustomerAccount();
    final URI location = URI.create("/tiny-ledger/customers/" + customerId);
    return ResponseEntity.created(location).body(customerId);
  }

  @PostMapping("/customers/{customerId}/transactions")
  public ResponseEntity<UUID> postTransaction(@PathVariable final UUID customerId, @RequestBody final CreateTransactionDTO transaction) {
    final UUID transactionId = ledgerService.storeCustomerTransaction(customerId, transaction);
    final URI location = URI.create("/tiny-ledger/transactions/" + transactionId);
    return ResponseEntity.created(location).body(transactionId);
  }

  @GetMapping("/customers/{customerId}/transactions")
  public ResponseEntity<List<TransactionDTO>> getTransactions(@PathVariable final UUID customerId) {
    return ResponseEntity.ok(ledgerService.getCustomerTransactions(customerId));
  }

  @GetMapping("/customers/{customerId}/transactions/{transactionId}")
  public ResponseEntity<TransactionDTO> getTransaction(@PathVariable final UUID customerId, @PathVariable final UUID transactionId) {
    return ResponseEntity.ok(ledgerService.getCustomerTransaction(customerId, transactionId));
  }

  @GetMapping("/customers/{customerId}/balance")
  public ResponseEntity<BigDecimal> getBalance(@PathVariable final UUID customerId) {
    return ResponseEntity.ok(ledgerService.getCustomerBalance(customerId));
  }


}

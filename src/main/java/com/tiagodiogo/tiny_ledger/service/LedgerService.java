package com.tiagodiogo.tiny_ledger.service;

import com.tiagodiogo.tiny_ledger.domain.dto.CreateTransactionDTO;
import com.tiagodiogo.tiny_ledger.domain.dto.TransactionDTO;
import com.tiagodiogo.tiny_ledger.domain.entity.CustomerAccount;
import com.tiagodiogo.tiny_ledger.domain.entity.Transaction;
import com.tiagodiogo.tiny_ledger.repository.LedgerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class LedgerService {

  public static final String CUSTOMER_ACCOUNT_NOT_FOUND = "Customer account not found";
  public static final String INSUFFICIENT_FUNDS = "Insufficient funds";
  public static final String INVALID_TRANSACTION_TYPE = "Invalid transaction type";
  public static final String TRANSACTION_NOT_FOUND = "Transaction not found";

  private final LedgerRepository ledgerRepository = LedgerRepository.getInstance();

  /**
   * Creates a new customer account with a unique ID, zero balance, and no transactions.
   *
   * @return the UUID of the newly created customer account
   */
  public UUID createCustomerAccount() {
    final UUID customerId = UUID.randomUUID();
    final CustomerAccount customerAccount = CustomerAccount.builder()
        .id(customerId)
        .balance(BigDecimal.ZERO)
        .transactions(new ArrayList<>())
        .build();
    ledgerRepository.storeCustomerAccount(customerId, customerAccount);
    log.info("Created Customer Account: {}", customerId);
    return customerId;
  }

  /**
   * Retrieves the balance of a customer account by its ID.
   *
   * @param customerId the UUID of the customer account
   * @return the balance of the customer account
   * @throws IllegalArgumentException if the customer account is not found
   */
  public BigDecimal getCustomerBalance(final UUID customerId) {
    return ledgerRepository.getCustomerAccount(customerId)
        .map(CustomerAccount::getBalance)
        .orElseThrow(() -> new IllegalArgumentException(CUSTOMER_ACCOUNT_NOT_FOUND));
  }

  /**
   * Stores a new transaction for a customer account.
   *
   * @param transaction the transaction data transfer object
   * @return the UUID of the newly created transaction
   * @throws IllegalArgumentException if the customer account is not found, if there are insufficient funds for a withdrawal, or if the transaction type is invalid
   */
  public synchronized UUID storeCustomerTransaction(final UUID customerId, final CreateTransactionDTO transaction) {
    final UUID transactionId = UUID.randomUUID();
    ledgerRepository.getCustomerAccount(customerId)
        .ifPresentOrElse(account -> {
          switch (transaction.getType()) {
            case DEPOSIT -> {
              final BigDecimal newBalance = account.getBalance().add(transaction.getAmount());
              account.setBalance(newBalance);
              account.getTransactions().add(Transaction.builder()
                  .id(transactionId)
                  .description(transaction.getDescription())
                  .amount(transaction.getAmount())
                  .type(transaction.getType())
                  .build());
            }
            case WITHDRAWAL -> {
              final BigDecimal newBalance = account.getBalance().subtract(transaction.getAmount());
              if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(INSUFFICIENT_FUNDS);
              }
              account.setBalance(newBalance);
              account.getTransactions().add(Transaction.builder()
                  .id(transactionId)
                  .description(transaction.getDescription())
                  .amount(transaction.getAmount())
                  .build());
            }
            default -> throw new IllegalArgumentException(INVALID_TRANSACTION_TYPE);
          }
    }, () -> {
      throw new IllegalArgumentException(CUSTOMER_ACCOUNT_NOT_FOUND);
    });
    log.info("Created customer transaction: {}", transactionId);
    return transactionId;
  }

  /**
   * Retrieves the list of transactions for a customer account by its ID.
   *
   * @param customerId the UUID of the customer account
   * @return the list of transaction data transfer objects
   * @throws IllegalArgumentException if the customer account is not found
   */
  public List<TransactionDTO> getCustomerTransactions(final UUID customerId) {
    return ledgerRepository.getCustomerAccount(customerId)
        .map(CustomerAccount::getTransactions)
        .orElseThrow(() -> new IllegalArgumentException(CUSTOMER_ACCOUNT_NOT_FOUND))
        .stream()
        .map(transaction -> TransactionDTO.builder()
            .id(transaction.getId())
            .description(transaction.getDescription())
            .amount(transaction.getAmount())
            .type(transaction.getType())
            .build())
        .toList();
  }

  /**
   * Retrieves a specific transaction for a customer account by its ID.
   *
   * @param customerId the UUID of the customer account
   * @param transactionId the UUID of the transaction
   * @return the transaction data transfer object
   * @throws IllegalArgumentException if the customer account or transaction is not found
   */
  public TransactionDTO getCustomerTransaction(final UUID customerId, final UUID transactionId) {
    return ledgerRepository.getCustomerAccount(customerId)
        .map(CustomerAccount::getTransactions)
        .orElseThrow(() -> new IllegalArgumentException(CUSTOMER_ACCOUNT_NOT_FOUND))
        .stream()
        .filter(transaction -> transaction.getId().equals(transactionId))
        .findFirst()
        .map(transaction -> TransactionDTO.builder()
            .id(transaction.getId())
            .description(transaction.getDescription())
            .amount(transaction.getAmount())
            .type(transaction.getType())
            .build())
        .orElseThrow(() -> new IllegalArgumentException(TRANSACTION_NOT_FOUND));
  }

}

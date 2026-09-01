package br.com.desafio.orders.ledger;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByWalletIdOrderByIdAsc(UUID walletId);

    List<LedgerEntry> findByOrderId(UUID orderId);

    boolean existsByOrderIdAndType(UUID orderId, LedgerEntryType type);
}

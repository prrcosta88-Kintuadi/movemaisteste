package br.com.desafio.orders.orders;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o WHERE " +
           "(:walletId IS NULL OR o.walletId = :walletId) AND " +
           "(:status IS NULL OR o.status = :status)")
    Page<Order> findByFilters(@Param("walletId") UUID walletId,
                              @Param("status") OrderStatus status,
                              Pageable pageable);
}
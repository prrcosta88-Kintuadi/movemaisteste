package br.com.desafio.orders.orders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.desafio.orders.catalog.Product;
import br.com.desafio.orders.catalog.ProductRepository;
import br.com.desafio.orders.ledger.LedgerEntryRepository;
import br.com.desafio.orders.orders.dto.CreateOrderRequest;
import br.com.desafio.orders.orders.dto.OrderItemRequest;
import br.com.desafio.orders.shared.error.ApiException;
import br.com.desafio.orders.shared.error.ErrorCode;
import br.com.desafio.orders.wallet.Wallet;
import br.com.desafio.orders.wallet.WalletRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock WalletRepository walletRepository;
    @Mock ProductRepository productRepository;
    @Mock LedgerEntryRepository ledgerEntryRepository;
    @Mock Clock clock;

    @InjectMocks
    OrderService orderService;

    @Test
    void shouldRejectDuplicateSku() {
        var request = new CreateOrderRequest(UUID.randomUUID(), List.of(
                new OrderItemRequest("SKU-MOUSE", 1),
                new OrderItemRequest("SKU-MOUSE", 2)
        ));

        ApiException ex = assertThrows(ApiException.class, () ->
                orderService.createOrder(request, "key-1"));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
    }

    @Test
    void shouldRejectInactiveProduct() {
        var walletId = UUID.randomUUID();
        var request = new CreateOrderRequest(walletId, List.of(
                new OrderItemRequest("SKU-ADAPTADOR", 1)
        ));

        when(productRepository.findBySkuIn(anyList()))
                .thenReturn(List.of(new Product("SKU-ADAPTADOR", "Adaptador", 3990, false)));

        ApiException ex = assertThrows(ApiException.class, () ->
                orderService.createOrder(request, "key-2"));

        assertEquals(ErrorCode.PRODUCT_INACTIVE, ex.getCode());
    }
}
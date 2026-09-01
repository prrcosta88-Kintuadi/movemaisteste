package br.com.desafio.orders.orders;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.desafio.orders.catalog.Product;
import br.com.desafio.orders.catalog.ProductRepository;
import br.com.desafio.orders.ledger.LedgerEntry;
import br.com.desafio.orders.ledger.LedgerEntryRepository;
import br.com.desafio.orders.ledger.LedgerEntryType;
import br.com.desafio.orders.orders.dto.CreateOrderRequest;
import br.com.desafio.orders.orders.dto.OrderItemRequest;
import br.com.desafio.orders.orders.dto.OrderItemResponse;
import br.com.desafio.orders.orders.dto.OrderListResponse;
import br.com.desafio.orders.orders.dto.OrderResponse;
import br.com.desafio.orders.orders.dto.OrderSummaryResponse;
import br.com.desafio.orders.shared.Money;
import br.com.desafio.orders.shared.error.ApiException;
import br.com.desafio.orders.shared.error.ErrorCode;
import br.com.desafio.orders.wallet.Wallet;
import br.com.desafio.orders.wallet.WalletRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final WalletRepository walletRepository;
    private final ProductRepository productRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        IdempotencyKeyRepository idempotencyKeyRepository,
                        WalletRepository walletRepository,
                        ProductRepository productRepository,
                        LedgerEntryRepository ledgerEntryRepository,
                        Clock clock) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.walletRepository = walletRepository;
        this.productRepository = productRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.clock = clock;
    }

    @Transactional
    public CreateOrderResult createOrder(CreateOrderRequest request, String idempotencyKey) {
        // 1. Validar idempotência
        String payloadHash = hashPayload(request);
        var existingKey = idempotencyKeyRepository.findByKeyValue(idempotencyKey);
        if (existingKey.isPresent()) {
            if (!existingKey.get().getPayloadHash().equals(payloadHash)) {
                throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT,
                        "Idempotency-Key ja usada com payload diferente.");
            }
            // Mesma chave + mesmo payload -> retornar pedido existente
            Order existingOrder = orderRepository.findById(existingKey.get().getOrderId())
                    .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "Pedido nao encontrado."));
            return new CreateOrderResult(toOrderResponse(existingOrder), true);
        }

        // 2. Validar SKU repetido
        Set<String> skus = new LinkedHashSet<>();
        for (var item : request.items()) {
            if (!skus.add(item.sku().toUpperCase())) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR,
                        "SKU repetido no mesmo pedido: " + item.sku());
            }
        }

        // 3. Buscar produtos
        List<String> skuList = request.items().stream()
                .map(i -> i.sku().toUpperCase())
                .collect(Collectors.toList());
        List<Product> products = productRepository.findBySkuIn(skuList);
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getSku, p -> p));

        // 4. Validar produtos
        long totalCents = 0;
        for (var itemReq : request.items()) {
            String sku = itemReq.sku().toUpperCase();
            Product product = productMap.get(sku);
            if (product == null) {
                throw new ApiException(ErrorCode.PRODUCT_NOT_FOUND,
                        "Produto nao encontrado: " + itemReq.sku());
            }
            if (!product.isActive()) {
                throw new ApiException(ErrorCode.PRODUCT_INACTIVE,
                        "Produto inativo: " + itemReq.sku());
            }
            totalCents += product.getPriceCents() * itemReq.quantity();
        }

        // 5. Buscar carteira com lock pessimista
        Wallet wallet = walletRepository.findByIdWithLock(request.walletId())
                .orElseThrow(() -> new ApiException(ErrorCode.WALLET_NOT_FOUND,
                        "Carteira " + request.walletId() + " nao encontrada."));

        // 6. Debitar carteira
        wallet.debit(totalCents);
        walletRepository.save(wallet);

        // 7. Criar pedido
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now(clock);
        Order order = new Order(orderId, request.walletId(), OrderStatus.CONFIRMED, totalCents, now);
        for (var itemReq : request.items()) {
            Product product = productMap.get(itemReq.sku().toUpperCase());
            order.addItem(new OrderItem(product.getSku(), product.getName(),
                    itemReq.quantity(), product.getPriceCents()));
        }
        orderRepository.save(order);

        // 8. Gravar lançamento na razão
        LedgerEntry ledger = new LedgerEntry(
                request.walletId(), orderId, LedgerEntryType.DEBIT, totalCents, now);
        ledgerEntryRepository.save(ledger);

        // 9. Salvar idempotency key
        idempotencyKeyRepository.save(
                new IdempotencyKey(idempotencyKey, payloadHash, orderId));

        return new CreateOrderResult(toOrderResponse(order), false);
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND,
                        "Pedido " + orderId + " nao encontrado."));

        // Já cancelado -> idempotente
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        // Verificar janela de 7 dias
        Instant now = Instant.now(clock);
        Instant deadline = order.getCreatedAt().plus(7, ChronoUnit.DAYS);
        if (now.isAfter(deadline)) {
            throw new ApiException(ErrorCode.CANCELLATION_WINDOW_EXPIRED,
                    "Janela de cancelamento expirada.");
        }

        // Buscar carteira com lock
        Wallet wallet = walletRepository.findByIdWithLock(order.getWalletId())
                .orElseThrow(() -> new ApiException(ErrorCode.WALLET_NOT_FOUND,
                        "Carteira nao encontrada."));

        // Creditar
        wallet.credit(order.getTotalCents());
        walletRepository.save(wallet);

        // Atualizar status
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Gravar lançamento REFUND
        LedgerEntry refund = new LedgerEntry(
                order.getWalletId(), orderId, LedgerEntryType.REFUND,
                order.getTotalCents(), now);
        ledgerEntryRepository.save(refund);
    }

    @Transactional(readOnly = true)
    public OrderListResponse listOrders(UUID walletId, OrderStatus status, int page, int size) {
        int effectiveSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, effectiveSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result = orderRepository.findByFilters(walletId, status, pageable);

        List<OrderSummaryResponse> content = result.getContent().stream()
                .map(o -> new OrderSummaryResponse(
                        o.getId().toString(),
                        o.getWalletId().toString(),
                        o.getStatus().name(),
                        Money.format(o.getTotalCents()),
                        o.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());

        return new OrderListResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private OrderResponse toOrderResponse(Order order) {
        var items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getSku(),
                        i.getName(),
                        i.getQuantity(),
                        Money.format(i.getUnitPriceCents()),
                        Money.format(i.getSubtotalCents())
                ))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId().toString(),
                order.getWalletId().toString(),
                order.getStatus().name(),
                Money.format(order.getTotalCents()),
                items,
                order.getCreatedAt().toString()
        );
    }

    private String hashPayload(CreateOrderRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = request.walletId().toString() + "|" +
                    request.items().stream()
                            .map(i -> i.sku() + ":" + i.quantity())
                            .collect(Collectors.joining(","));
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public record CreateOrderResult(OrderResponse response, boolean isReplay) {}
}
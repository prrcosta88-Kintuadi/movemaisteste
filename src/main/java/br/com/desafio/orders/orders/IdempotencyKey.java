package br.com.desafio.orders.orders;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_key")
public class IdempotencyKey {

    @Id
    @Column(name = "key_value", nullable = false, length = 64)
    private String keyValue;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    protected IdempotencyKey() {
    }

    public IdempotencyKey(String keyValue, String payloadHash, UUID orderId) {
        this.keyValue = keyValue;
        this.payloadHash = payloadHash;
        this.orderId = orderId;
    }

    public String getKeyValue() { return keyValue; }
    public String getPayloadHash() { return payloadHash; }
    public UUID getOrderId() { return orderId; }
}
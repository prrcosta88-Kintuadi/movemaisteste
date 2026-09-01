package br.com.desafio.orders.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Produto do catalogo. Ja vem pronto no starter - voce nao precisa alterar.
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sku", nullable = false, unique = true, length = 40)
    private String sku;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** Preco unitario em centavos. */
    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Product() {
        // exigido pelo JPA
    }

    public Product(String sku, String name, long priceCents, boolean active) {
        this.sku = sku;
        this.name = name;
        this.priceCents = priceCents;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public boolean isActive() {
        return active;
    }
}

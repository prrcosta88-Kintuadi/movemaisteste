package br.com.desafio.orders.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    /**
     * Busca varios SKUs de uma vez. Existe para voce nao precisar fazer um SELECT por item
     * do pedido (N+1). Use se fizer sentido no seu desenho.
     */
    List<Product> findBySkuIn(Collection<String> skus);
}

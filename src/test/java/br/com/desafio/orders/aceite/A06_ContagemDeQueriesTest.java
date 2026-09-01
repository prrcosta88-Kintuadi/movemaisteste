package br.com.desafio.orders.aceite;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.desafio.orders.aceite.support.AcceptanceTestBase;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Guarda contra N+1 na listagem: uma pagina de 20 pedidos nao pode custar 20 idas ao banco.
 */
@Tag("aceite")
@DisplayName("R6 - a listagem nao pode ter N+1")
class A06_ContagemDeQueriesTest extends AcceptanceTestBase {

    private static final String W_N_MAIS_UM = "00000000-0000-0000-0000-000000000061";
    private static final int PEDIDOS = 20;
    private static final long LIMITE_DE_QUERIES = 6L;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("listar 20 pedidos gasta poucas queries, nao uma por pedido")
    void listagemDeVintePedidosNaoDisparaUmaQueryPorPedido() {
        for (int i = 0; i < PEDIDOS; i++) {
            createOrderExpectingSuccess(W_N_MAIS_UM, items(item(SKU_CABO, 1), item(SKU_MOUSE, 1)));
        }

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        ResponseEntity<JsonNode> response = listOrders("walletId=" + W_N_MAIS_UM + "&page=0&size=20");

        long queries = statistics.getPrepareStatementCount();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("content")).hasSize(PEDIDOS);
        assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(PEDIDOS);

        assertThat(queries)
                .as("a listagem de uma pagina de %d pedidos disparou %d comandos SQL. "
                        + "Esperado: no maximo %d (tipicamente 1 count + 1 select). "
                        + "Causas comuns: associacao @ManyToOne EAGER, ou o DTO tocando "
                        + "uma colecao LAZY item a item.", PEDIDOS, queries, LIMITE_DE_QUERIES)
                .isLessThanOrEqualTo(LIMITE_DE_QUERIES);
    }
}

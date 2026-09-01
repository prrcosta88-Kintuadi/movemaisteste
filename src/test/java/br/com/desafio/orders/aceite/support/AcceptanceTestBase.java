package br.com.desafio.orders.aceite.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.desafio.orders.OrdersApplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base da bateria de aceite do avaliador.
 *
 * <p>Estes testes conversam APENAS pelo contrato HTTP público (e pela tabela
 * {@code ledger_entry}, que já vem pronta no starter). Eles não conhecem nenhuma
 * classe sua: você é livre para desenhar os pacotes, serviços e entidades como quiser.
 *
 * <p>Rodar: {@code mvn test -Paceite}
 */
@Tag("aceite")
@SpringBootTest(
        classes = {OrdersApplication.class, AcceptanceClockConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:orders-aceite;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
                "spring.jpa.properties.hibernate.generate_statistics=true",
                "logging.level.org.hibernate.SQL=warn"
        })
public abstract class AcceptanceTestBase {

    // SKUs da massa de dados (data.sql)
    protected static final String SKU_CABO = "SKU-CABO-USB";      // R$   10,00
    protected static final String SKU_MOUSE = "SKU-MOUSE";        // R$   49,90
    protected static final String SKU_TECLADO = "SKU-TECLADO";    // R$  119,90
    protected static final String SKU_MONITOR = "SKU-MONITOR";    // R$  899,00
    protected static final String SKU_INATIVO = "SKU-ADAPTADOR";  // R$   39,90 (active = false)

    protected static final String ORDERS_PATH = "/api/v1/orders";

    private static final AtomicLong KEY_SEQUENCE = new AtomicLong();

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void resetClockBeforeEachTest() {
        clock.reset();
    }

    // ------------------------------------------------------------------ HTTP

    protected ResponseEntity<JsonNode> createOrder(String idempotencyKey,
                                                   String walletId,
                                                   List<Map<String, Object>> items) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("walletId", walletId);
        body.put("items", items);

        return rest.exchange(ORDERS_PATH, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
    }

    /** Cria um pedido esperando 201 e devolve o id gerado. */
    protected String createOrderExpectingSuccess(String walletId, List<Map<String, Object>> items) {
        ResponseEntity<JsonNode> response = createOrder(newKey(), walletId, items);
        assertThat(response.getStatusCode())
                .as("criacao de pedido deveria retornar 201; corpo: %s", response.getBody())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().get("id").asText();
    }

    protected ResponseEntity<JsonNode> cancelOrder(String orderId) {
        return rest.exchange(ORDERS_PATH + "/{orderId}/cancellation", HttpMethod.POST,
                HttpEntity.EMPTY, JsonNode.class, orderId);
    }

    protected ResponseEntity<JsonNode> listOrders(String queryString) {
        String url = queryString == null || queryString.isBlank()
                ? ORDERS_PATH
                : ORDERS_PATH + "?" + queryString;
        return rest.getForEntity(url, JsonNode.class);
    }

    protected long balanceCents(String walletId) {
        ResponseEntity<JsonNode> response =
                rest.getForEntity("/api/v1/wallets/{walletId}", JsonNode.class, walletId);
        assertThat(response.getStatusCode())
                .as("GET da carteira %s deveria retornar 200", walletId)
                .isEqualTo(HttpStatus.OK);
        return centsOf(response.getBody().get("balance"));
    }

    // ------------------------------------------------------------- utilidades

    protected static Map<String, Object> item(String sku, Object quantity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("sku", sku);
        item.put("quantity", quantity);
        return item;
    }

    @SafeVarargs
    protected static List<Map<String, Object>> items(Map<String, Object>... entries) {
        return new ArrayList<>(List.of(entries));
    }

    /** Aceita "289.70" e 289.70 (string ou número); compara sempre em centavos. */
    protected static long centsOf(JsonNode moneyNode) {
        assertThat(moneyNode).as("campo monetario ausente na resposta").isNotNull();
        return new BigDecimal(moneyNode.asText()).movePointRight(2).longValueExact();
    }

    protected static String codeOf(ResponseEntity<JsonNode> response) {
        JsonNode body = response.getBody();
        assertThat(body).as("resposta de erro sem corpo").isNotNull();
        assertThat(body.get("code")).as("resposta de erro sem o campo 'code'; corpo: %s", body).isNotNull();
        return body.get("code").asText();
    }

    protected static String newKey() {
        return "aceite-" + KEY_SEQUENCE.incrementAndGet() + "-" + UUID.randomUUID();
    }

    /** Conta lançamentos na razão da carteira (tabela fornecida pelo starter). */
    protected long countLedger(String walletId, String type, long amountCents) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ledger_entry "
                        + "WHERE CAST(wallet_id AS VARCHAR) = ? AND type = ? AND amount_cents = ?",
                Long.class, walletId, type, amountCents);
        return count == null ? 0L : count;
    }

    protected static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package br.com.desafio.orders.wallet;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Exemplo de teste de integracao do starter (roda em {@code mvn test}).
 * Use-o como modelo para os seus.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletControllerTest {

    private static final String CARTEIRA_DEMO = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("devolve a carteira com o saldo formatado em reais")
    void devolveCarteiraComSaldoFormatado() {
        ResponseEntity<JsonNode> response =
                rest.getForEntity("/api/v1/wallets/{id}", JsonNode.class, CARTEIRA_DEMO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id").asText()).isEqualTo(CARTEIRA_DEMO);
        assertThat(response.getBody().get("ownerName").asText()).isEqualTo("Ana Souza");
        assertThat(response.getBody().get("balance").asText()).isEqualTo("500.00");
    }

    @Test
    @DisplayName("carteira inexistente devolve 404 com o corpo de erro padrao")
    void carteiraInexistenteDevolve404() {
        ResponseEntity<JsonNode> response =
                rest.getForEntity("/api/v1/wallets/{id}", JsonNode.class, UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code").asText()).isEqualTo("WALLET_NOT_FOUND");
        assertThat(response.getBody().get("status").asInt()).isEqualTo(404);
        assertThat(response.getBody().get("path").asText()).contains("/api/v1/wallets/");
    }
}

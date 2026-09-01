package br.com.desafio.orders.aceite;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.desafio.orders.aceite.support.AcceptanceTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Tag("aceite")
@DisplayName("R4 - idempotencia da criacao de pedido")
class A02_IdempotenciaTest extends AcceptanceTestBase {

    private static final String W_REPLAY = "00000000-0000-0000-0000-000000000021";
    private static final String W_CONFLITO = "00000000-0000-0000-0000-000000000022";
    private static final String W_CHAVES = "00000000-0000-0000-0000-000000000023";

    @Test
    @DisplayName("mesma chave + mesmo payload: devolve 200 com o MESMO pedido e nao debita de novo")
    void mesmaChaveMesmoPayloadNaoDuplicaODebito() {
        String chave = newKey();
        long saldoAntes = balanceCents(W_REPLAY);

        ResponseEntity<JsonNode> primeira =
                createOrder(chave, W_REPLAY, items(item(SKU_TECLADO, 1)));
        assertThat(primeira.getStatusCode())
                .as("corpo: %s", primeira.getBody())
                .isEqualTo(HttpStatus.CREATED);
        String idOriginal = primeira.getBody().get("id").asText();

        long saldoAposPrimeira = balanceCents(W_REPLAY);
        assertThat(saldoAposPrimeira).isEqualTo(saldoAntes - 11990L);

        ResponseEntity<JsonNode> replay =
                createOrder(chave, W_REPLAY, items(item(SKU_TECLADO, 1)));

        assertThat(replay.getStatusCode())
                .as("o replay de uma Idempotency-Key ja usada deve devolver 200 OK; corpo: %s", replay.getBody())
                .isEqualTo(HttpStatus.OK);
        assertThat(replay.getBody().get("id").asText())
                .as("o replay tem que devolver o pedido original, nao um novo")
                .isEqualTo(idOriginal);
        assertThat(centsOf(replay.getBody().get("total"))).isEqualTo(11990L);

        assertThat(balanceCents(W_REPLAY))
                .as("o replay NAO pode debitar a carteira de novo")
                .isEqualTo(saldoAposPrimeira);

        assertThat(countLedger(W_REPLAY, "DEBIT", 11990L))
                .as("o replay nao pode gerar um segundo lancamento na razao")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("mesma chave + payload diferente: devolve 409 IDEMPOTENCY_KEY_CONFLICT")
    void mesmaChavePayloadDiferenteRetorna409() {
        String chave = newKey();

        ResponseEntity<JsonNode> primeira =
                createOrder(chave, W_CONFLITO, items(item(SKU_MOUSE, 1)));
        assertThat(primeira.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        long saldoAposPrimeira = balanceCents(W_CONFLITO);

        ResponseEntity<JsonNode> conflitante =
                createOrder(chave, W_CONFLITO, items(item(SKU_MOUSE, 2)));

        assertThat(conflitante.getStatusCode())
                .as("corpo: %s", conflitante.getBody())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(codeOf(conflitante)).isEqualTo("IDEMPOTENCY_KEY_CONFLICT");
        assertThat(balanceCents(W_CONFLITO)).isEqualTo(saldoAposPrimeira);
    }

    @Test
    @DisplayName("chaves diferentes com o mesmo payload criam dois pedidos distintos")
    void chavesDiferentesCriamPedidosDistintos() {
        long saldoAntes = balanceCents(W_CHAVES);

        ResponseEntity<JsonNode> primeira =
                createOrder(newKey(), W_CHAVES, items(item(SKU_MOUSE, 1)));
        ResponseEntity<JsonNode> segunda =
                createOrder(newKey(), W_CHAVES, items(item(SKU_MOUSE, 1)));

        assertThat(primeira.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segunda.getBody().get("id").asText())
                .isNotEqualTo(primeira.getBody().get("id").asText());

        assertThat(balanceCents(W_CHAVES))
                .as("dois pedidos distintos debitam duas vezes")
                .isEqualTo(saldoAntes - (2 * 4990L));
    }
}

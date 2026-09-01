package br.com.desafio.orders.aceite;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.desafio.orders.aceite.support.AcceptanceTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Tag("aceite")
@DisplayName("R5 - cancelamento, estorno e janela de 7 dias")
class A03_CancelamentoTest extends AcceptanceTestBase {

    private static final String W_CANCELA = "00000000-0000-0000-0000-000000000031";
    private static final String W_CANCELA_2X = "00000000-0000-0000-0000-000000000032";
    private static final String W_EXPIRADA = "00000000-0000-0000-0000-000000000033";
    private static final String W_VALIDA = "00000000-0000-0000-0000-000000000034";
    private static final String W_CANCELADO_E_EXPIRADO = "00000000-0000-0000-0000-000000000035";

    @Test
    @DisplayName("cancelar devolve 204, estorna o saldo integral e grava REFUND na razao")
    void cancelaEstornaSaldoEGravaNaRazao() {
        long saldoInicial = balanceCents(W_CANCELA);

        String pedido = createOrderExpectingSuccess(W_CANCELA, items(item(SKU_TECLADO, 2)));
        assertThat(balanceCents(W_CANCELA)).isEqualTo(saldoInicial - 23980L);

        ResponseEntity<JsonNode> cancelamento = cancelOrder(pedido);

        assertThat(cancelamento.getStatusCode())
                .as("corpo: %s", cancelamento.getBody())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(balanceCents(W_CANCELA))
                .as("o cancelamento tem que devolver o valor integral do pedido")
                .isEqualTo(saldoInicial);
        assertThat(countLedger(W_CANCELA, "REFUND", 23980L))
                .as("deveria existir 1 lancamento REFUND de 239,80 na tabela ledger_entry")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("cancelar duas vezes e idempotente: 204 nas duas e um unico estorno")
    void segundoCancelamentoNaoEstornaDeNovo() {
        long saldoInicial = balanceCents(W_CANCELA_2X);

        String pedido = createOrderExpectingSuccess(W_CANCELA_2X, items(item(SKU_MOUSE, 1)));

        assertThat(cancelOrder(pedido).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        long saldoAposPrimeiroCancelamento = balanceCents(W_CANCELA_2X);
        assertThat(saldoAposPrimeiroCancelamento).isEqualTo(saldoInicial);

        ResponseEntity<JsonNode> segunda = cancelOrder(pedido);

        assertThat(segunda.getStatusCode())
                .as("cancelar um pedido ja cancelado continua sendo 204; corpo: %s", segunda.getBody())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(balanceCents(W_CANCELA_2X))
                .as("o segundo cancelamento NAO pode creditar a carteira de novo")
                .isEqualTo(saldoAposPrimeiroCancelamento);
        assertThat(countLedger(W_CANCELA_2X, "REFUND", 4990L))
                .as("nao pode existir um segundo lancamento REFUND")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("pedido inexistente devolve 404 ORDER_NOT_FOUND")
    void pedidoInexistenteRetorna404() {
        ResponseEntity<JsonNode> response = cancelOrder(UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(codeOf(response)).isEqualTo("ORDER_NOT_FOUND");
    }

    @Test
    @DisplayName("fora da janela de 7 dias devolve 422 CANCELLATION_WINDOW_EXPIRED e nao estorna")
    void foraDaJanelaDeSeteDiasRecusa() {
        long saldoInicial = balanceCents(W_EXPIRADA);

        String pedido = createOrderExpectingSuccess(W_EXPIRADA, items(item(SKU_MOUSE, 1)));
        long saldoAposPedido = balanceCents(W_EXPIRADA);
        assertThat(saldoAposPedido).isEqualTo(saldoInicial - 4990L);

        clock.advance(Duration.ofDays(7).plusMinutes(5));

        ResponseEntity<JsonNode> response = cancelOrder(pedido);

        assertThat(response.getStatusCode())
                .as("corpo: %s (a regra tem que ler o instante atual do bean Clock)", response.getBody())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(codeOf(response)).isEqualTo("CANCELLATION_WINDOW_EXPIRED");
        assertThat(balanceCents(W_EXPIRADA))
                .as("pedido fora da janela nao estorna")
                .isEqualTo(saldoAposPedido);
    }

    @Test
    @DisplayName("dentro da janela de 7 dias ainda cancela")
    void dentroDaJanelaDeSeteDiasCancela() {
        long saldoInicial = balanceCents(W_VALIDA);

        String pedido = createOrderExpectingSuccess(W_VALIDA, items(item(SKU_MOUSE, 1)));

        clock.advance(Duration.ofDays(6).plusHours(23));

        assertThat(cancelOrder(pedido).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(balanceCents(W_VALIDA)).isEqualTo(saldoInicial);
    }

    @Test
    @DisplayName("pedido ja cancelado continua devolvendo 204 mesmo depois da janela expirar")
    void pedidoJaCanceladoForaDaJanelaContinua204() {
        long saldoInicial = balanceCents(W_CANCELADO_E_EXPIRADO);

        String pedido = createOrderExpectingSuccess(W_CANCELADO_E_EXPIRADO, items(item(SKU_MOUSE, 1)));
        assertThat(cancelOrder(pedido).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        clock.advance(Duration.ofDays(30));

        ResponseEntity<JsonNode> response = cancelOrder(pedido);

        assertThat(response.getStatusCode())
                .as("a checagem de 'ja cancelado' vem ANTES da checagem da janela; corpo: %s", response.getBody())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(balanceCents(W_CANCELADO_E_EXPIRADO)).isEqualTo(saldoInicial);
    }
}

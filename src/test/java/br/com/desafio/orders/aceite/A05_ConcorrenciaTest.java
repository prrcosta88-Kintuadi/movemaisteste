package br.com.desafio.orders.aceite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.desafio.orders.aceite.support.AcceptanceTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Tag("aceite")
@DisplayName("R7 - concorrencia: a carteira nao pode ficar negativa")
class A05_ConcorrenciaTest extends AcceptanceTestBase {

    /** Saldo de R$ 100,00 na massa de dados. */
    private static final String W_CONCORRENCIA = "00000000-0000-0000-0000-000000000051";

    /** SKU_CABO custa R$ 10,00 -> cabem exatamente 10 pedidos. */
    private static final int REQUISICOES_SIMULTANEAS = 20;
    private static final int PEDIDOS_QUE_CABEM = 10;

    @Test
    @DisplayName("20 pedidos simultaneos na mesma carteira: 10 confirmam, 10 sao recusados, saldo final zero")
    void naoPermiteSaldoNegativoSobConcorrencia() throws Exception {
        long saldoInicial = balanceCents(W_CONCORRENCIA);
        assertThat(saldoInicial)
                .as("a carteira de concorrencia deve comecar com R$ 100,00 (massa de dados intacta)")
                .isEqualTo(10_000L);

        ExecutorService pool = Executors.newFixedThreadPool(REQUISICOES_SIMULTANEAS);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<ResponseEntity<JsonNode>>> futures = new ArrayList<>();
        List<ResponseEntity<JsonNode>> respostas = new ArrayList<>();

        try {
            for (int i = 0; i < REQUISICOES_SIMULTANEAS; i++) {
                futures.add(pool.submit(() -> {
                    largada.await();
                    return createOrder(newKey(), W_CONCORRENCIA, items(item(SKU_CABO, 1)));
                }));
            }
            largada.countDown();

            for (Future<ResponseEntity<JsonNode>> future : futures) {
                respostas.add(future.get(120, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        List<Integer> statusRecebidos = new ArrayList<>();
        Set<String> idsCriados = new HashSet<>();
        int criados = 0;
        int recusados = 0;

        for (ResponseEntity<JsonNode> resposta : respostas) {
            statusRecebidos.add(resposta.getStatusCode().value());

            if (resposta.getStatusCode() == HttpStatus.CREATED) {
                criados++;
                idsCriados.add(resposta.getBody().get("id").asText());
            } else if (resposta.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                recusados++;
                assertThat(codeOf(resposta)).isEqualTo("INSUFFICIENT_BALANCE");
            }
        }

        assertThat(statusRecebidos)
                .as("nenhuma requisicao pode devolver 5xx nem qualquer status fora de 201/422. "
                        + "Status recebidos: %s", statusRecebidos)
                .allMatch(status -> status == 201 || status == 422);

        assertThat(criados)
                .as("cabem exatamente %d pedidos de R$ 10,00 em R$ 100,00. Status recebidos: %s",
                        PEDIDOS_QUE_CABEM, statusRecebidos)
                .isEqualTo(PEDIDOS_QUE_CABEM);

        assertThat(recusados)
                .as("os %d pedidos restantes tem que ser recusados por saldo",
                        REQUISICOES_SIMULTANEAS - PEDIDOS_QUE_CABEM)
                .isEqualTo(REQUISICOES_SIMULTANEAS - PEDIDOS_QUE_CABEM);

        assertThat(idsCriados)
                .as("cada pedido confirmado tem que ter um id proprio")
                .hasSize(PEDIDOS_QUE_CABEM);

        assertThat(balanceCents(W_CONCORRENCIA))
                .as("saldo final tem que ser exatamente zero - se sobrou dinheiro, houve perda de atualizacao")
                .isZero();

        assertThat(countLedger(W_CONCORRENCIA, "DEBIT", 1_000L))
                .as("a razao tem que ter exatamente %d lancamentos DEBIT de R$ 10,00", PEDIDOS_QUE_CABEM)
                .isEqualTo(PEDIDOS_QUE_CABEM);
    }
}

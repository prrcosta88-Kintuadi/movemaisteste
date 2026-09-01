package br.com.desafio.orders.aceite;

import java.util.List;
import java.util.Map;
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
@DisplayName("R1/R2/R3 - criacao de pedido, debito e validacoes")
class A01_CriacaoDePedidoTest extends AcceptanceTestBase {

    private static final String W_FELIZ = "00000000-0000-0000-0000-000000000011";
    private static final String W_SALDO_BAIXO = "00000000-0000-0000-0000-000000000012";
    private static final String W_DIVERSOS = "00000000-0000-0000-0000-000000000013";

    @Test
    @DisplayName("cria o pedido, debita a carteira e grava o lancamento DEBIT na razao")
    void criaPedidoDebitaCarteiraEGravaNaRazao() {
        long saldoAntes = balanceCents(W_FELIZ);
        long ledgerAntes = countLedger(W_FELIZ, "DEBIT", 28970L);

        ResponseEntity<JsonNode> response = createOrder(newKey(), W_FELIZ,
                items(item(SKU_TECLADO, 2), item(SKU_MOUSE, 1)));

        assertThat(response.getStatusCode())
                .as("corpo: %s", response.getBody())
                .isEqualTo(HttpStatus.CREATED);

        JsonNode body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.get("id")).as("campo 'id' ausente").isNotNull();
        UUID.fromString(body.get("id").asText()); // o id do pedido e um UUID
        assertThat(body.get("walletId").asText()).isEqualTo(W_FELIZ);
        assertThat(body.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(body.get("createdAt")).as("campo 'createdAt' ausente").isNotNull();

        // 2 x 119,90 + 1 x 49,90 = 289,70
        assertThat(centsOf(body.get("total"))).isEqualTo(28970L);

        JsonNode itens = body.get("items");
        assertThat(itens).as("campo 'items' ausente").isNotNull();
        assertThat(itens.isArray()).isTrue();
        assertThat(itens).hasSize(2);

        long somaSubtotais = 0L;
        for (JsonNode itemNode : itens) {
            assertThat(itemNode.get("sku")).as("item sem 'sku'").isNotNull();
            assertThat(itemNode.get("quantity")).as("item sem 'quantity'").isNotNull();
            assertThat(itemNode.get("unitPrice")).as("item sem 'unitPrice'").isNotNull();
            assertThat(itemNode.get("subtotal")).as("item sem 'subtotal'").isNotNull();

            long unitario = centsOf(itemNode.get("unitPrice"));
            int quantidade = itemNode.get("quantity").asInt();
            long subtotal = centsOf(itemNode.get("subtotal"));
            assertThat(subtotal)
                    .as("subtotal do item %s deveria ser preco unitario x quantidade", itemNode.get("sku").asText())
                    .isEqualTo(unitario * quantidade);
            somaSubtotais += subtotal;
        }
        assertThat(somaSubtotais).as("total deveria ser a soma dos subtotais").isEqualTo(28970L);

        assertThat(balanceCents(W_FELIZ))
                .as("a carteira deveria ter sido debitada em 289,70")
                .isEqualTo(saldoAntes - 28970L);

        assertThat(countLedger(W_FELIZ, "DEBIT", 28970L))
                .as("deveria existir exatamente 1 lancamento DEBIT novo de 289,70 na tabela ledger_entry")
                .isEqualTo(ledgerAntes + 1);
    }

    @Test
    @DisplayName("saldo insuficiente devolve 422 INSUFFICIENT_BALANCE e nao debita nada")
    void saldoInsuficienteRetorna422SemDebitar() {
        long saldoAntes = balanceCents(W_SALDO_BAIXO);

        ResponseEntity<JsonNode> response = createOrder(newKey(), W_SALDO_BAIXO,
                items(item(SKU_MONITOR, 1)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(codeOf(response)).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(balanceCents(W_SALDO_BAIXO))
                .as("nada pode ser debitado quando o pedido e recusado")
                .isEqualTo(saldoAntes);
    }

    @Test
    @DisplayName("sku inexistente devolve 404 PRODUCT_NOT_FOUND")
    void skuInexistenteRetorna404() {
        long saldoAntes = balanceCents(W_DIVERSOS);

        ResponseEntity<JsonNode> response = createOrder(newKey(), W_DIVERSOS,
                items(item(SKU_MOUSE, 1), item("SKU-QUE-NAO-EXISTE", 1)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(codeOf(response)).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(balanceCents(W_DIVERSOS)).isEqualTo(saldoAntes);
    }

    @Test
    @DisplayName("produto inativo devolve 422 PRODUCT_INACTIVE")
    void produtoInativoRetorna422() {
        long saldoAntes = balanceCents(W_DIVERSOS);

        ResponseEntity<JsonNode> response = createOrder(newKey(), W_DIVERSOS,
                items(item(SKU_INATIVO, 1)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(codeOf(response)).isEqualTo("PRODUCT_INACTIVE");
        assertThat(balanceCents(W_DIVERSOS)).isEqualTo(saldoAntes);
    }

    @Test
    @DisplayName("carteira inexistente devolve 404 WALLET_NOT_FOUND")
    void carteiraInexistenteRetorna404() {
        ResponseEntity<JsonNode> response = createOrder(newKey(), UUID.randomUUID().toString(),
                items(item(SKU_MOUSE, 1)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(codeOf(response)).isEqualTo("WALLET_NOT_FOUND");
    }

    @Test
    @DisplayName("payload invalido devolve 400 VALIDATION_ERROR")
    void payloadInvalidoRetorna400() {
        long saldoAntes = balanceCents(W_DIVERSOS);

        assertValidacao("lista de itens vazia",
                createOrder(newKey(), W_DIVERSOS, List.of()));

        assertValidacao("quantidade zero",
                createOrder(newKey(), W_DIVERSOS, items(item(SKU_MOUSE, 0))));

        assertValidacao("quantidade negativa",
                createOrder(newKey(), W_DIVERSOS, items(item(SKU_MOUSE, -3))));

        assertValidacao("quantidade acima do limite de 100",
                createOrder(newKey(), W_DIVERSOS, items(item(SKU_MOUSE, 101))));

        assertValidacao("sku nulo",
                createOrder(newKey(), W_DIVERSOS, items(item(null, 1))));

        assertValidacao("walletId ausente",
                createOrder(newKey(), null, items(item(SKU_MOUSE, 1))));

        assertValidacao("sku repetido no mesmo pedido",
                createOrder(newKey(), W_DIVERSOS, items(item(SKU_MOUSE, 1), item(SKU_MOUSE, 2))));

        assertValidacao("header Idempotency-Key ausente",
                createOrder(null, W_DIVERSOS, items(item(SKU_MOUSE, 1))));

        assertThat(balanceCents(W_DIVERSOS))
                .as("nenhuma requisicao invalida pode ter debitado a carteira")
                .isEqualTo(saldoAntes);
    }

    @Test
    @DisplayName("o preco cobrado vem do catalogo, nunca do que o cliente mandou")
    void precoVemDoCatalogoNaoDoRequest() {
        long saldoAntes = balanceCents(W_DIVERSOS);

        Map<String, Object> itemComPrecoForjado = item(SKU_TECLADO, 1);
        itemComPrecoForjado.put("unitPrice", "0.01");
        itemComPrecoForjado.put("subtotal", "0.01");

        ResponseEntity<JsonNode> response =
                createOrder(newKey(), W_DIVERSOS, items(itemComPrecoForjado));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(centsOf(response.getBody().get("total")))
                .as("o total tem que ser o preco de catalogo (119,90), nao o enviado pelo cliente")
                .isEqualTo(11990L);
        assertThat(balanceCents(W_DIVERSOS)).isEqualTo(saldoAntes - 11990L);
    }

    private void assertValidacao(String cenario, ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode())
                .as("cenario '%s' deveria devolver 400; corpo: %s", cenario, response.getBody())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(codeOf(response))
                .as("cenario '%s'", cenario)
                .isEqualTo("VALIDATION_ERROR");
    }
}

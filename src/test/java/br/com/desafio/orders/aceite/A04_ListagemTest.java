package br.com.desafio.orders.aceite;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.desafio.orders.aceite.support.AcceptanceTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Tag("aceite")
@DisplayName("R6 - listagem paginada e filtros")
class A04_ListagemTest extends AcceptanceTestBase {

    private static final String W_PAGINACAO = "00000000-0000-0000-0000-000000000041";
    private static final String W_STATUS = "00000000-0000-0000-0000-000000000042";
    private static final String W_ORDENACAO = "00000000-0000-0000-0000-000000000043";

    @Test
    @DisplayName("pagina e devolve o envelope com page, size, totalElements e totalPages")
    void paginaCorretamente() {
        for (int i = 0; i < 5; i++) {
            createOrderExpectingSuccess(W_PAGINACAO, items(item(SKU_CABO, 1)));
        }

        ResponseEntity<JsonNode> primeiraPagina = listOrders("walletId=" + W_PAGINACAO + "&size=2&page=0");
        assertThat(primeiraPagina.getStatusCode())
                .as("corpo: %s", primeiraPagina.getBody())
                .isEqualTo(HttpStatus.OK);

        JsonNode body = primeiraPagina.getBody();
        assertThat(body.get("content")).as("campo 'content' ausente").isNotNull();
        assertThat(body.get("content")).hasSize(2);
        assertThat(body.get("page").asInt()).isEqualTo(0);
        assertThat(body.get("size").asInt()).isEqualTo(2);
        assertThat(body.get("totalElements").asLong()).isEqualTo(5L);
        assertThat(body.get("totalPages").asInt()).isEqualTo(3);

        ResponseEntity<JsonNode> ultimaPagina = listOrders("walletId=" + W_PAGINACAO + "&size=2&page=2");
        assertThat(ultimaPagina.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ultimaPagina.getBody().get("content")).hasSize(1);
        assertThat(ultimaPagina.getBody().get("page").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("filtra por status")
    void filtraPorStatus() {
        String a = createOrderExpectingSuccess(W_STATUS, items(item(SKU_CABO, 1)));
        createOrderExpectingSuccess(W_STATUS, items(item(SKU_CABO, 1)));
        createOrderExpectingSuccess(W_STATUS, items(item(SKU_CABO, 1)));

        assertThat(cancelOrder(a).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<JsonNode> cancelados = listOrders("walletId=" + W_STATUS + "&status=CANCELLED");
        assertThat(cancelados.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelados.getBody().get("totalElements").asLong()).isEqualTo(1L);
        assertThat(cancelados.getBody().get("content").get(0).get("id").asText()).isEqualTo(a);
        assertThat(cancelados.getBody().get("content").get(0).get("status").asText()).isEqualTo("CANCELLED");

        ResponseEntity<JsonNode> confirmados = listOrders("walletId=" + W_STATUS + "&status=CONFIRMED");
        assertThat(confirmados.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmados.getBody().get("totalElements").asLong()).isEqualTo(2L);
    }

    @Test
    @DisplayName("ordena do mais recente para o mais antigo e usa size padrao 20")
    void ordenaDoMaisRecenteParaOMaisAntigoEUsaSizePadrao() {
        List<String> criadosEmOrdem = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            criadosEmOrdem.add(createOrderExpectingSuccess(W_ORDENACAO, items(item(SKU_CABO, 1))));
            sleepQuietly(30);
        }

        ResponseEntity<JsonNode> response = listOrders("walletId=" + W_ORDENACAO);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = response.getBody();
        assertThat(body.get("size").asInt()).as("size padrao deve ser 20").isEqualTo(20);
        assertThat(body.get("page").asInt()).isEqualTo(0);
        assertThat(body.get("totalElements").asLong()).isEqualTo(3L);

        JsonNode content = body.get("content");
        assertThat(content).hasSize(3);
        assertThat(content.get(0).get("id").asText())
                .as("o primeiro da lista tem que ser o pedido mais recente")
                .isEqualTo(criadosEmOrdem.get(2));
        assertThat(content.get(2).get("id").asText())
                .as("o ultimo da lista tem que ser o pedido mais antigo")
                .isEqualTo(criadosEmOrdem.get(0));

        JsonNode primeiro = content.get(0);
        assertThat(primeiro.get("walletId").asText()).isEqualTo(W_ORDENACAO);
        assertThat(primeiro.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(centsOf(primeiro.get("total"))).isEqualTo(1000L);
        assertThat(primeiro.get("createdAt")).as("campo 'createdAt' ausente no item da listagem").isNotNull();
    }
}

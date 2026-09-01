# Desafio técnico — Desenvolvedor(a) Backend Pleno (Java + Spring Boot)

Você vai completar uma API de **pedidos pagos com carteira pré-paga**.

O projeto já sobe, já tem banco com dados, um endpoint funcionando como referência de estilo
e uma bateria de testes que diz exatamente quando você terminou. O que falta é a regra de negócio.

| | |
|---|---|
| **Esforço esperado** | ~2 horas |
| **Prazo de entrega** | combinado com o RH (você não precisa fazer de uma vez só) |
| **Onde rodar** | sua máquina |
| **Pré-requisitos** | JDK 17 ou superior e Maven 3.8+ (`java -version` e `mvn -version`) |
| **Banco** | H2 em memória — **não precisa instalar nada** |

---

## 1. Regras do jogo

- **Use o que você usa no dia a dia.** Bibliotecas, IDE, atalhos, snippets: à vontade.
- **Pode usar IA** (Copilot, ChatGPT, Claude...). Só entenda o que ficou no código: na entrevista
  você vai abrir o seu PR e explicar as decisões linha a linha. Código que você não consegue
  defender conta contra, tenha vindo de onde tiver vindo.
- **Não é para caprichar em tudo.** Duas horas não dão para fazer o ideal. Faça as escolhas
  que você faria num sprint real e **anote no `SOLUCAO.md` o que você deixou de fora e por quê** —
  essa anotação vale tanto quanto código.
- **Prefira entregar menos, funcionando e testado, do que tudo pela metade.**

---

## 2. Como rodar

```bash
# subir a aplicação (http://localhost:8080)
mvn spring-boot:run

# rodar os SEUS testes
mvn test

# rodar a bateria de aceite (a "definição de pronto" deste desafio)
mvn test -Paceite

# rodar tudo junto
mvn test -Ptudo
```

Console do banco: <http://localhost:8080/h2-console>
JDBC URL `jdbc:h2:mem:orders` · usuário `sa` · senha em branco.

O arquivo [`requests.http`](requests.http) tem uma requisição pronta para cada cenário
(funciona no IntelliJ e na extensão *REST Client* do VS Code).

> Na primeira execução o Maven baixa as dependências — isso pode levar alguns minutos.
> Se `mvn test` passar num clone limpo, seu ambiente está ok.

---

## 3. O domínio

Uma loja vende produtos de um **catálogo**. O cliente paga com uma **carteira pré-paga**
(saldo carregado antecipadamente). Não existe cartão, boleto nem gateway externo:
o pedido é pago debitando a carteira no ato.

Três invariantes que o negócio não abre mão:

1. **O saldo nunca pode ficar negativo.**
2. **Todo movimento de dinheiro deixa rastro** na razão (`ledger_entry`), na mesma transação
   que altera o saldo.
3. **`saldo_atual == saldo_inicial − Σ(DEBIT) + Σ(REFUND)`** — sempre.

Dinheiro é `long` em **centavos**. `double` e `float` são proibidos para valor monetário.

---

## 4. O que já está pronto (não precisa refazer)

```
src/main/java/br/com/desafio/orders/
├── OrdersApplication.java        ← não renomeie nem mova
├── config/ClockConfig.java       ← bean Clock (leia a seção 6, R5)
├── catalog/                      ← Product + ProductRepository
├── wallet/                       ← Wallet (com debit/credit), repositório,
│                                    service e controller  ← EXEMPLO DE ESTILO
├── ledger/                       ← LedgerEntry + repositório
└── shared/
    ├── Money.java                ← centavos <-> "289.70"
    └── error/                    ← ApiError, ApiException, ErrorCode,
                                     GlobalExceptionHandler (contrato de erro pronto)
```

- `src/main/resources/data.sql` — catálogo e carteiras. **Não altere os UUIDs nem os SKUs.**
- `GET /api/v1/wallets/{id}` é o único endpoint implementado. Ele é o seu modelo de
  `controller → service → repository`.

Tudo isso é código do projeto como qualquer outro: **você pode alterar o que precisar**
(inclusive as entidades fornecidas), desde que a bateria de aceite continue passando.

### Catálogo (`data.sql`)

| SKU | Produto | Preço | Ativo |
|---|---|---:|:---:|
| `SKU-CABO-USB` | Cabo USB-C 1m | 10,00 | sim |
| `SKU-ADAPTADOR` | Adaptador HDMI (descontinuado) | 39,90 | **não** |
| `SKU-MOUSE` | Mouse sem fio | 49,90 | sim |
| `SKU-SUPORTE` | Suporte para notebook | 89,90 | sim |
| `SKU-TECLADO` | Teclado mecânico | 119,90 | sim |
| `SKU-WEBCAM` | Webcam HD | 159,00 | sim |
| `SKU-HEADSET` | Headset com microfone | 259,90 | sim |
| `SKU-DOCK` | Dock station USB-C | 459,00 | sim |
| `SKU-MONITOR` | Monitor 24 polegadas | 899,00 | sim |

Carteiras para você testar na mão: `...0001` (Ana Souza, R$ 500,00) e
`...0002` (Bruno Lima, R$ 30,00). As demais são reservadas para a bateria de aceite.

---

## 5. O que você precisa implementar

Três endpoints. O modelo de `Order` / `OrderItem` (e o que mais você julgar necessário)
é **você** quem desenha.

### R1 — `POST /api/v1/orders` · criar pedido

- Calcula o total a partir do **preço do catálogo**, nunca do que veio no corpo (**R3**).
- Debita a carteira e grava **um lançamento `DEBIT`** em `ledger_entry` com o valor total,
  na mesma transação.
- O pedido nasce com status `CONFIRMED`. O id do pedido é um **UUID**.

### R2 — validações

| Situação | HTTP | `code` |
|---|:---:|---|
| `walletId` ausente/inválido, `items` vazio, `quantity` fora de `1..100`, `sku` nulo/vazio, **SKU repetido no mesmo pedido**, header `Idempotency-Key` ausente | 400 | `VALIDATION_ERROR` |
| carteira não existe | 404 | `WALLET_NOT_FOUND` |
| algum SKU não existe | 404 | `PRODUCT_NOT_FOUND` |
| algum produto está inativo | 422 | `PRODUCT_INACTIVE` |
| saldo não cobre o total | 422 | `INSUFFICIENT_BALANCE` |

Em **qualquer** um desses casos nada pode ser persistido: nem pedido, nem débito, nem lançamento.

### R4 — idempotência

O header `Idempotency-Key` é **obrigatório** na criação e a chave precisa ser **persistida**.

- **Mesma chave + mesmo payload** → `200 OK` com **o mesmo pedido** da primeira chamada,
  **sem novo débito e sem novo lançamento**.
- **Mesma chave + payload diferente** → `409 IDEMPOTENCY_KEY_CONFLICT`.
- Chaves diferentes → pedidos diferentes, mesmo com payload idêntico.

O escopo da chave é global (a chave sozinha identifica a requisição).

### R5 — `POST /api/v1/orders/{orderId}/cancellation` · cancelar

- `204 No Content`, sem corpo.
- Credita a carteira com **o valor integral** do pedido e grava **um lançamento `REFUND`**.
- O pedido passa para o status `CANCELLED`.
- **Idempotente:** cancelar de novo devolve `204` e **não** estorna uma segunda vez.
- Pedido inexistente → `404 ORDER_NOT_FOUND`.
- **Janela de 7 dias:** só é possível cancelar até 7 dias corridos após a criação do pedido.
  Depois disso → `422 CANCELLATION_WINDOW_EXPIRED`, sem estorno.
- **Ordem das checagens:** se o pedido **já está cancelado**, a resposta é `204`
  mesmo que a janela já tenha expirado.

> ⚠️ **Leia o instante atual do bean `Clock`** (`Instant.now(clock)`), nunca de
> `Instant.now()` / `LocalDateTime.now()` / `new Date()`. A bateria de aceite troca esse bean
> por um relógio controlável para simular a passagem dos 7 dias. Regra que lê o relógio do
> sistema direto não tem como ser testada — e vai reprovar aqui.

### R6 — `GET /api/v1/orders` · listar

Parâmetros, todos opcionais: `walletId`, `status` (`CONFIRMED` \| `CANCELLED`),
`page` (padrão `0`), `size` (padrão `20`, máximo `100` — acima disso, use 100).

- Ordenação: **mais recente primeiro** (`createdAt` decrescente).
- Cada item da lista é um **resumo** (sem os itens do pedido).
- **Sem N+1:** listar uma página de 20 pedidos não pode custar 20 idas ao banco.
  A bateria mede isso (limite: 6 comandos SQL para uma página de 20).

### R7 — concorrência

Duas requisições simultâneas na mesma carteira **não podem** furar o saldo.

Com R$ 100,00 na carteira e 20 pedidos simultâneos de R$ 10,00, o resultado tem que ser:
**10 pedidos com `201`, 10 com `422 INSUFFICIENT_BALANCE`, saldo final zero e 10 lançamentos
na razão.** Nenhum `5xx`.

Pense em o que acontece quando duas transações leem o mesmo saldo antes de qualquer uma gravar.
A escolha da estratégia é sua — mas escreva no `SOLUCAO.md` qual você usou e por quê.

### R8 — testes

Escreva os **seus** testes (eles rodam em `mvn test`). Não repita a bateria de aceite:
mostre o que **você** acha que precisa de cobertura. Qualidade importa mais que quantidade —
5 testes certeiros valem mais que 30 triviais.

---

## 6. Contrato da API

### `POST /api/v1/orders`

```http
POST /api/v1/orders
Content-Type: application/json
Idempotency-Key: 7c1f0f52-9a1e-4f3e-9d0a-3c9a1b2d4e5f

{
  "walletId": "00000000-0000-0000-0000-000000000001",
  "items": [
    { "sku": "SKU-TECLADO", "quantity": 2 },
    { "sku": "SKU-MOUSE",   "quantity": 1 }
  ]
}
```

**`201 Created`**

```json
{
  "id": "6f9619ff-8b86-d011-b42d-00cf4fc964ff",
  "walletId": "00000000-0000-0000-0000-000000000001",
  "status": "CONFIRMED",
  "total": "289.70",
  "items": [
    { "sku": "SKU-TECLADO", "name": "Teclado mecânico", "quantity": 2, "unitPrice": "119.90", "subtotal": "239.80" },
    { "sku": "SKU-MOUSE",   "name": "Mouse sem fio",    "quantity": 1, "unitPrice": "49.90",  "subtotal": "49.90"  }
  ],
  "createdAt": "2026-09-01T13:42:07.913Z"
}
```

Valores monetários saem como **string com 2 casas decimais** (use `Money.format`).
Datas em **ISO-8601 UTC**.

### `POST /api/v1/orders/{orderId}/cancellation`

Sem corpo na requisição. **`204 No Content`**, sem corpo na resposta.

### `GET /api/v1/orders?walletId=...&status=...&page=0&size=20`

**`200 OK`**

```json
{
  "content": [
    {
      "id": "6f9619ff-8b86-d011-b42d-00cf4fc964ff",
      "walletId": "00000000-0000-0000-0000-000000000001",
      "status": "CONFIRMED",
      "total": "289.70",
      "createdAt": "2026-09-01T13:42:07.913Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Corpo de erro (já implementado)

```json
{
  "timestamp": "2026-09-01T13:45:02.104Z",
  "status": 422,
  "code": "INSUFFICIENT_BALANCE",
  "message": "Saldo insuficiente para concluir o pedido.",
  "path": "/api/v1/orders"
}
```

Para gerar qualquer um deles, basta lançar:

```java
throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE, "Saldo insuficiente para concluir o pedido.");
```

---

## 7. A bateria de aceite é a sua definição de pronto

```bash
mvn test -Paceite
```

Ela vive em `src/test/java/br/com/desafio/orders/aceite/` e conversa **só pelo contrato HTTP**
(e pela tabela `ledger_entry`). Ela não conhece nenhuma classe sua — o desenho interno é livre.

| Classe | O que cobre |
|---|---|
| `A01_CriacaoDePedidoTest` | R1, R2, R3 |
| `A02_IdempotenciaTest` | R4 |
| `A03_CancelamentoTest` | R5 |
| `A04_ListagemTest` | R6 |
| `A05_ConcorrenciaTest` | R7 |
| `A06_ContagemDeQueriesTest` | R6 (N+1) |

**Não altere os arquivos dessa pasta.** Se você achar que algum teste está errado, mantenha
o teste como está e escreva o seu argumento no `SOLUCAO.md` — questionar um requisito com um
bom argumento conta a favor.

Passar 100% da bateria não é obrigatório para ser aprovado, e passar 100% também não aprova
sozinho: metade da nota está em como o código ficou.

---

## 8. Como você vai ser avaliado

| Peso | Critério |
|---:|---|
| 40% | **Funciona:** bateria de aceite verde |
| 20% | **Desenho e legibilidade:** camadas, nomes, coesão, tratamento de erro, sem código morto |
| 15% | **Seus testes:** cobrem a regra que importa, rodam rápido, falham por um motivo claro |
| 15% | **Consistência de dados:** fronteira transacional, atomicidade, concorrência, razão batendo com o saldo |
| 10% | **Entrega:** commits legíveis, `SOLUCAO.md`, projeto que sobe num clone limpo |

Chamam atenção positivamente: *value objects* em vez de `long` solto espalhado, regra de negócio
na entidade em vez de tudo no service, teste que documenta a intenção, e um `SOLUCAO.md`
que assume os trade-offs em vez de escondê-los.

---

## 9. O que **não** é necessário

Não gastamos avaliação com nada disso — e fazer não soma pontos:

- autenticação, autorização, JWT
- Docker, CI, Kubernetes, deploy
- Swagger/OpenAPI, Postman collection
- front-end de qualquer tipo
- cache, mensageria, observabilidade, métricas
- cadastro de produtos ou de carteiras (a massa do `data.sql` basta)
- trocar o H2 por outro banco
- migrations (Flyway/Liquibase)

---

## 10. Entrega

1. Crie um **`SOLUCAO.md`** na raiz com (não passe de 1 página):
   - como rodar, se você mudou alguma coisa no processo;
   - **as decisões de projeto** e o porquê — em especial a estratégia de concorrência (R7)
     e a de idempotência (R4);
   - **o que ficou de fora** e o que você faria com mais tempo;
   - qualquer ponto do enunciado que você considera ambíguo ou errado.
2. Faça commits com mensagens que contem a história (não um único "final").
3. Envie **um repositório Git** (link público ou `.zip` com a pasta `.git` incluída) para quem
   te enviou o desafio.

Boa sorte — e obrigado pelo tempo investido.

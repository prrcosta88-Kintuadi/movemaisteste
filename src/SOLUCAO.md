# SOLUCAO.md - Desafio Backend Pleno

## Decisões de Design

### 1. Idempotência
Usei o padrão de Idempotency-Key no header para garantir que requisições repetidas não criem pedidos duplicados. A chave é armazenada com o hash do payload para detectar conflitos.

### 2. Lock Pessimista
Usei `@Lock(LockModeType.PESSIMISTIC_WRITE)` na consulta da carteira para evitar problemas de concorrência em cenários de alta demanda.

### 3. Organização de Pacotes
- `dto`: Objetos de transferência de dados (records)
- `model`: Entidades JPA
- `repository`: Interfaces de acesso a dados
- `service`: Lógica de negócio

### 4. Transactions
Todas as operações que modificam estado estão com `@Transactional` para garantir atomicidade.

## O que ficou de fora

1. **Testes unitários**: Criei apenas os testes necessários para validar a lógica principal.
2. **Validações avançadas**: Validações simples de SKU e quantidade, mas poderia ter mais regras de negócio.
3. **Logs estruturados**: Não implementei logs detalhados para simplificar.

## Pontos ambíguos

1. O enunciado não especifica claramente o formato dos erros de validação.
2. Não está claro se o cancelamento deve verificar saldo da carteira para estorno.
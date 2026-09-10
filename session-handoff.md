# Session Handoff — stats-service

## Current Objective

- `epic-004` (raiz) `done` — `feat-001..009` `done`. `feat-010` (retry de aplicação, achado do
  RabbitMQ 4.3+) acrescentada e fechada nesta sessão. Nenhuma feature pendente neste harness.
- Branch / commit: `develop` @ merge de `feature/SV-268` (PR #36).

## Completed This Session (2026-09-10)

- [x] **`feat-010` fechada** (story SV-268, subtasks SV-269..272, PR #36 feature->develop,
      CI+SonarCloud verdes): retry de aplicação para `BetEventListener`. Achado real de
      `infra/feat-002` (teste de resiliência de `epic-007`, outro repositório): a partir do
      RabbitMQ 4.3 (versão real deste projeto, 4.3.5), `nack(requeue=true)` — o que o Spring AMQP
      faz por padrão em qualquer falha de listener — deixou de contar para `x-delivery-limit` da
      fila quorum `stats.bet-events`. Confirmado ao vivo: derrubar `postgres-stats` fazia o
      consumidor falhar indefinidamente sem nunca cair na DLQ, quebrando o objetivo do
      `epic-007`. Fix: `spring.rabbitmq.listener.simple.retry` (3 tentativas, bloco DEFAULT de
      `application.yml`, sem duplicar por profile) — após esgotar, o
      `RejectAndDontRequeueRecoverer` padrão rejeita com `requeue=false`, que sempre morta-letra
      independente da contagem do broker.
- [x] Teste novo (`BetEventListenerRetryIntegrationTest`) prova o caminho "falha transitória
      esgota tentativas → DLQ", que os 2 testes de DLQ existentes (caminho de reject imediato por
      erro de dado) não cobriam.
- [x] Achado de processo, corrigido: as 4 subtasks tinham sido mescladas localmente numa sessão
      anterior sem passar por PR/CI real — corrigido nesta sessão, branches empurradas pro GitHub
      e o PR `feature/SV-268 -> develop` passou pela CI real (incluindo SonarCloud) antes do
      merge.
- [x] Dezenas de branches antigas já mescladas (deste e de outros repositórios) limpas local e
      remotamente, a pedido do usuário.

## Verification Evidence

| Check | Command | Result | Notes |
|---|---|---|---|
| Build/test | `./mvnw -q verify` | pass | JaCoCo gate incluso; os 2 testes de DLQ pré-existentes continuam passando |
| Local harness | `./init.sh` | pass | |
| CI (full gate) | GitHub Actions + SonarCloud | pass | PR #36 |

## Blockers / Risks

- Nenhum.

## Next Session Startup

1. Ler `../../CLAUDE.md` e o `CLAUDE.md` deste serviço.
2. `feature_list.json` deste harness: todas as features `done` (`feat-001..010`). Nenhum trabalho
   pendente aqui até surgir novo achado cross-service ou nova feature.
3. Rodar `./init.sh` (deve passar).

## Recommended Next Step

- Nenhum próximo passo pendente neste harness. Qualquer outro serviço Java que vier a consumir
  fila própria vai precisar do mesmo `spring.rabbitmq.listener.simple.retry` (RabbitMQ 4.3+ do
  ambiente) — ver `docs/DECISIONS-LOG.md`/`docs/services/infra.md` na raiz.

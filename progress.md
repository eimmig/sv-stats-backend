# Log de Progresso — stats-service

## Estado Atual (Current State)

**Última atualização:** 2026-09-06
**Feature ativa:** nenhuma (`feat-001` `done`, `feat-002` liberada)

## Status

### O que está pronto

- [x] Harness deste serviço criado.
- [x] **`feat-001` (Setup do projeto + consumidor RabbitMQ) — `done` em 2026-09-06.** Bootstrap
      Spring Boot 4.1.1/Java 25/Maven (groupId `com.stakevault.betting`, compartilhado com
      `auth-service`/`bets-service`), Postgres dev/test/prod, schema-per-tenant (Flyway lazy) +
      filtro `X-Tenant-Id` + rota admin `X-Admin-Api-Key`, gate JaCoCo 80%, i18n (3 locales),
      health checks do Actuator (`db`+`rabbit`), `.env.example`+logging estruturado ECS, e o
      consumidor RabbitMQ dos eventos `BetCreated`/`BetSettled` (`adapter/in/messaging`) com
      validação de schema em runtime — ainda sem persistência (`FACT_BET`/`PROCESSED_EVENT` são
      `feat-002`/`feat-003`). Story SV-110, 10 subtasks (SV-111..120). Evidência completa em
      `feature_list.json` (campo `evidence`).

### Em andamento

- Nenhuma feature em andamento.

### Próximos passos (Next Steps)

1. `feat-002` — modelo OLAP (esquema estrela) + `PROCESSED_EVENT` para idempotência do consumo.

## Bloqueios / Riscos

- Nenhum. O bloqueio original ("depende de `bets-service` publicar o evento") está resolvido —
  `bets-service epic-003` está `done`, publicando `BetCreated`/`BetSettled` de verdade.

## Decisões tomadas

- Build tool: **Maven**. Arquitetura: **hexagonal** (domain/application/adapter). Ambas
  decididas em `../../docs/CONVENTIONS.md`, não específicas desta sessão.
- RabbitMQ Testcontainers/`Declarable` beans só entraram em `feat-001.9` (junto do consumidor),
  não em `feat-001.2` (bootstrap) — mantém o bootstrap idêntico ao padrão mínimo já usado em
  `bets-service`, sem acoplar infraestrutura de mensageria a uma feature que ainda não a usa.
- Falha de validação de schema no consumidor usa `AmqpRejectAndDontRequeueException` (rejeita
  sem reenfileirar, cai direto na DLQ), não o `x-delivery-limit`/retry automático da quorum
  queue — reservado para falha genuinamente transitória (achado real, ver `docs/CONVENTIONS.md`).

## Arquivos modificados nesta sessão

- `CLAUDE.md`, `feature_list.json`, `init.sh`, `progress.md`, `session-handoff.md` — criados
  (sessão de harness).
- `feat-001` (2026-09-06): pom.xml, esqueleto hexagonal completo, `application.yml`,
  `.env.example`, `messages*.properties`, `src/main/resources/contracts/*.schema.json`,
  ~30 classes Java (`domain/model`, `domain/port`, `application`, `adapter/in/web`,
  `adapter/in/messaging`, `config`) e os testes correspondentes — ver `feature_list.json` para o
  detalhe por subtask.
- `../../docs/API-CONTRACTS.md`, `../../docs/CONVENTIONS.md`, `../../docs/services/
  stats-service.md` (repositório raiz `sv-harness`) atualizados nos commits das subtasks
  `feat-001.9`/`feat-001.10`.

## Evidência de conclusão

- `./init.sh` (`mvn verify`) verde localmente com Docker ativo — 41 testes.
- CI verde em todas as 9 PRs de subtask e na PR de story→develop (#10), incluindo SonarCloud
  (4 achados reais corrigidos antes do merge — ver `feature_list.json`).
- Detalhe completo (verificação real, divergência do plano, defeitos encontrados, skills usadas)
  no campo `evidence` de `feat-001` em `feature_list.json`.

## Notas para a próxima sessão

Ver `../../docs/services/stats-service.md` para o esquema estrela completo e as chaves Redis
antes de começar `feat-002`. O consumidor RabbitMQ (`feat-001.9`) já prova que a mensagem chega,
valida e loga — `feat-002`/`feat-003` só precisam plugar a persistência real (`FACT_BET`,
`PROCESSED_EVENT`) no lugar do `log.info` atual do `BetEventListener`.

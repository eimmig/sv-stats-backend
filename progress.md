# Log de Progresso — stats-service

## Estado Atual (Current State)

**Última atualização:** 2026-09-06
**Feature ativa:** nenhuma (`feat-001`/`feat-002` `done`, `feat-003` liberada)

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

- [x] **`feat-002` (Modelo OLAP + idempotência) — `done` em 2026-09-06.** Migration Flyway
      (`fact_bet` + 6 dimensões + `processed_event`, `UNIQUE` real em `event_id`), multi-tenancy
      do Hibernate (`CurrentTenantIdentifierResolver`/`MultiTenantConnectionProvider` portados de
      `bets-service`, `TenantSchemaFilter` passa a exigir `X-Tenant-Id` em rotas de negócio), e a
      camada completa de persistência (`domain`+`adapter`) para os 8 conceitos — ainda sem
      wiring no `BetEventListener` (`feat-003`). Story SV-124, 4 subtasks (SV-125..128).
      Evidência completa em `feature_list.json`.

### Em andamento

- Nenhuma feature em andamento.

### Próximos passos (Next Steps)

1. `feat-003` — consumo dos eventos: *insert* em `BetCreated`, *upsert* em `BetSettled`, check de
   `PROCESSED_EVENT` antes de processar. Plugar no `BetEventListener` (`feat-001.9`) já existente.

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
- Multi-tenancy do Hibernate só entrou em `feat-002.2` (junto com as primeiras entidades JPA),
  não antes — mesma sequência real que `bets-service feat-002.2` seguiu (confirmado lendo o
  histórico daquele serviço antes de replicar). A prova de isolamento real entre tenants só
  ficou completa em `feat-002.3`, quando passou a existir uma entidade (`FactBet`) para isolar.
- `DimDate` é a única dimensão sem `id` vindo do evento — localizada por chave natural
  (dia/mês/ano), criada sob demanda na primeira aposta daquele dia (`feat-003`).

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

## Arquivos modificados nesta sessão (`feat-002`)

- Migration `V20260906233946__create_olap_schema.sql`.
- `config/{SchemaMultiTenantConnectionProvider,TenantIdentifierResolver,
  TenantHibernatePropertiesCustomizer}.java`, `TenantSchemaFilter` (enforcement),
  `messages*.properties` (chave `missing-tenant-id`).
- `domain/model/{BetStatus,FactBet,DimDate,DimBettingHouse,DimSport,DimLeague,DimMarket,
  DimTipster,ProcessedEvent}.java`, `domain/port/out/*Repository.java` (8 ports).
- `adapter/out/persistence/*JpaEntity.java`, `*SpringDataRepository.java`, `Jpa*Repository.java`
  (8 entidades + adapters) e os testes de integração correspondentes.
- `feature_list.json`, `CHANGELOG.md` deste serviço.

## Evidência de conclusão (`feat-002`)

- `./init.sh` (`mvn verify`) verde com Docker ativo — 53 testes.
- CI verde nas 3 PRs de subtask e na PR de story→develop (#14), incluindo SonarCloud (2 achados
  reais corrigidos antes do merge: `CHANGELOG.md` sem as linhas de SV-124..128 — perdidas num
  `git reset --hard` usado para corrigir um erro de branch não relacionado — e `java:S5778`).
- Detalhe completo no campo `evidence` de `feat-002` em `feature_list.json`.

## Notas para a próxima sessão

`feat-003` (consumo dos eventos) é a próxima. O `BetEventListener` (`feat-001.9`) já valida
schema e loga — falta plugar a lógica real: resolver/criar as 6 dimensões (upsert-if-missing por
id, exceto `DimDate` por chave natural), checar `PROCESSED_EVENT` antes de processar (idempotência
real via `existsByEventId`, inserir na mesma transação), *insert* em `FACT_BET` no `BetCreated`
(`status=pending`) e *upsert* por `betId` no `BetSettled` — sem falhar se o `BetCreated`
correspondente ainda não foi processado (mensagens fora de ordem, RN06 nunca inclui `pending` em
agregação). Ver `../../docs/services/stats-service.md` para o desenho completo.

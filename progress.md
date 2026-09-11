# Log de Progresso — stats-service

## Estado Atual (Current State)

**Última atualização:** 2026-09-11
**Feature ativa:** nenhuma — `feat-001`..`feat-017` `done`. Fecha `epic-018` da raiz.

## `feat-017` fechada — segmentos byLeague/byTipster (2026-09-11)

Fecha `epic-018` da raiz (escopo novo, fora do backlog original do TCC1, pedido do usuário
2026-09-10). `GET /api/v1/statistics` ganha 2 novos arrays no bundle — `byLeague`/`byTipster`,
mesmo formato `{dimensionId, dimensionName, metrics}` de `bySport`/`byMarket`/`byBettingHouse`
(`feat-006`) — fechando a lacuna documentada desde aquela feature (`leagueId`/`tipsterId` só
estreitavam os outros segmentos como filtro, nunca tiveram agrupamento próprio).

3 subtasks (story SV-358): `feat-017.1` (SV-359, `aggregateByLeague`/`aggregateByTipster` —
mirror exato de `aggregateBySport`/`aggregateByMarket`; `aggregateByTipster` exclui
`f.tipsterId IS NULL`, mesmo padrão de `aggregateByBetType.betType IS NOT NULL`, já que
`tipsterId` é opcional em `FACT_BET` diferente de `leagueId`), `feat-017.2` (SV-360,
`calculateByLeague`/`calculateByTipster` + `StatisticsDashboard` + cache-aside completo —
`getByLeague`/`getByTipster` com chaves `segment:league`/`segment:tipster`, `evict()` estendido
para incluir as 2 chaves novas), `feat-017.3` (SV-361, fechamento formal).

**2 achados MINOR do Plan Reviewer, ambos resolvidos no código**: (1) nomeação das chaves de
cache — seguiu o padrão majoritário `sport`/`market`/`house` (não `byBetType`, a única exceção
já existente) — `league`/`tipster`. (2) `evict()` precisava incluir as 2 chaves novas ou
`byLeague`/`byTipster` cacheados ficariam obsoletos após `BetSettled` até o TTL de segurança de
1h expirar — coberto por teste de integração dedicado
(`RedisMetricsCacheRepositoryIntegrationTest`) que prova a lacuna não existe, em vez de só
confiar na revisão manual.

`Delivery Reviewer`/`Test Suite Auditor`/`Persistence Auditor` (passe próprio, sem subagentes —
independência reduzida, declarada) rodados contra o diff completo (18 arquivos): todos `PASS`.
`./init.sh` verde. CI+SonarCloud verdes nas 3 PRs de subtask (#55/#56/#57) e na PR
`feature/SV-358 -> develop` (#58). `docs/API-CONTRACTS.md`/`docs/services/stats-service.md`
(repositório raiz) atualizados confirmando aderência ao planejado. Libera `epic-019` (web —
menu por cadastro).

## `feat-016` fechada — GET /api/v1/statistics/daily (2026-09-11)

Fecha `epic-016` da raiz (escopo novo, fora do backlog original do TCC1, pedido do usuário
2026-09-10). Novo endpoint `GET /api/v1/statistics/daily`: mesmos 7 filtros opcionais de
`GET /api/v1/statistics`, granularidade diária (`DimDate.day/month/year`, mesma mecânica de
`aggregateByMonth`), array esparso (só dias com pelo menos 1 aposta liquidada) ordenado por data
ascendente, sem cache-aside. Contrato já escrito antes do código em `docs/STATISTICS.md`/
`docs/API-CONTRACTS.md` (sessão de planejamento anterior) — implementação final confirmada
aderente, sem divergência.

3 subtasks (story SV-354): `feat-016.1` (SV-355, agregação diária via JPQL — `aggregateByDay`),
`feat-016.2` (SV-356, `calculateDaily`/controller — reaproveita a regra RN04 de roi=ZERO via
helper `roiOf` extraído de `toMetrics`), `feat-016.3` (SV-357, fechamento formal).

**Achado MINOR do Plan Reviewer, confirmado sem problema**: `FUNCTION('make_date', d.year,
d.month, d.day)` no `SELECT` combinado com `GROUP BY` nas colunas cruas era uma combinação nova
neste codebase (`findOrderedSettledProfits` já usava `FUNCTION()` sem agregação;
`aggregateByMonth` já usava `GROUP BY` sem `FUNCTION()`) — funcionou de primeira no teste de
integração real (Testcontainers Postgres), sem precisar do fallback (`LocalDate.of` client-side)
previsto no `plan_review`. Documentado em `docs/CONVENTIONS.md` (raiz) para reaproveitamento
direto por futura agregação por data.

`Delivery Reviewer`/`Test Suite Auditor`/`Persistence Auditor` (passe próprio, sem subagentes —
independência reduzida, declarada) rodados contra o diff completo (14 arquivos): todos `PASS`.
`./init.sh` verde. CI+SonarCloud verdes nas PRs #51/#52/#53 (subtask→feature) e #54
(feature→develop). Achado de processo corrigido durante o fechamento desta sessão: a primeira
tentativa marcou `feat-016.3` e `feat-016` `done` na mesma edição do JSON — corrigido antes do
commit (regra "não pular o estado Review", ver `CLAUDE.md` raiz), separando em duas
edições/`--sync-status` (subtask `done` → story `Review`; feature `done` numa edição posterior →
story `Done`).

## `feat-014` fechada — build/push de imagem Docker pro GHCR, validado de verdade (2026-09-10)

`feat-014.1`/`feat-014.2` já estavam `done` de uma sessão anterior (job no `ci.yml`, permissions
conferidas, CHANGELOG), mas a validação real ("push em main, job verde, imagem publicada") nunca
tinha ocorrido — a branch `main` deste serviço nunca tinha recebido merge de `develop`. Fechado
nesta sessão retomando o trabalho: encontrado como feature `in-progress` bloqueando o WIP deste
harness ao tentar iniciar `epic-014` da raiz (extensão do dashboard), decisão confirmada com o
usuário antes de agir (fechar isso primeiro em vez de deixar pendente).

`develop` promovido pra `main` por PR (mesmo padrão já usado por `auth-service`). O push real
revelou um achado de verdade, não hipotético: a etapa SonarCloud do `pipeline` falhou **só** na
branch `main` ("QUALITY GATE STATUS: FAILED"), bloqueando `build-and-push-image`
(`needs: pipeline`). Investigado a fundo via API do SonarCloud (token de `tools/.sonar.env`) em
vez de assumir causa: `qualitygates/project_status?analysisId=...` devolveu `status: "NONE"` com
`conditions: []` — zero condições reprovadas, não é problema de qualidade real. Confirmado contra
o código-fonte oficial do SonarQube (`ProjectStatusAction.java`) que `NONE` "é retornado quando
não há quality gate associada àquela análise", e contra o `sonar-scanner-engine`
(`QualityGateCheck.java`) que trata qualquer status != `OK` (inclusive `NONE`) como `FAILED` —
por isso o log mostrava falha sem nenhuma condição real. Causa: primeira análise de sempre da
branch `main`, sem baseline de "New Code" pra comparar. `gh run rerun` no mesmo commit **não**
resolveu (testado, descarta race condition simples) — corrigido só depois do usuário ajustar o
New Code Definition do projeto no dashboard do SonarCloud (sem API pública de escrita pra essa
config). Novo push (commit vazio) passou limpo, imagem confirmada no log do próprio job
(`ghcr.io/eimmig/sv-stats-backend:latest`+`:<sha>`, digest `sha256:e47e6424...`). Achado
registrado em `docs/CI-CD.md` (raiz) — outros serviços que ainda não fizeram o primeiro merge
`develop`→`main` podem bater no mesmo problema.

## `feat-013` fechada — DIM_TEAM escopado por esporte + GET /api/v1/statistics/teams (2026-09-10)

Addendum descoberto planejando a tela "Buscar Estatísticas" em `apps/web` (`epic-012` da raiz):
o Plan Reviewer daquela feature sinalizou que `DIM_TEAM` não tinha nenhum endpoint de listagem —
sem catálogo em `bets-service` (`team1`/`team2` são texto livre por aposta), o frontend não tinha
como saber quais `teamId` existiam pra montar o autocomplete de time. O Plan Reviewer também
sinalizou um residual não-bloqueante (mesmo nome de time em esportes diferentes colidiria no
autocomplete, já que `DIM_TEAM` não tinha FK de esporte) e aceitou deixá-lo pra depois. Usuário
leu o residual e decidiu o contrário: quis a correção agora.

3 subtasks (story SV-299): `feat-013.1` (SV-300, migration `sport_id` NOT NULL em `dim_team` +
chave natural composta `(name, sportId)`, `DimensionResolver.resolveTeam(name, sportId)`),
`feat-013.2` (SV-301, `GET /api/v1/statistics/teams?sportId=<uuid>` — sportId obrigatório, 400
RFC 7807 via `MissingRequiredStatisticsFilterException` reaproveitada de `feat-012`), `feat-013.3`
(SV-302, fechamento formal). Desvio de plano aceito: o endpoint entrou dentro do
`StatisticsController` já existente em vez de um `TeamsController` novo — mesmo limite hexagonal
(`adapter/in`→`port/in`→`port/out`), sem duplicar classe de controller pra uma rota só.

Achado real do Test Suite Auditor, corrigido no escopo de `feat-013.3`: a `UNIQUE(name, sport_id)`
nova nunca tinha teste que provasse a constraint no banco — só o caminho de aplicação (que já
evita duplicata antes do `save()`) era exercitado. Teste novo
(`shouldRejectDuplicateNameAndSportAtTheDatabaseLevel`) adicionado, mesmo padrão de
`JpaProcessedEventRepositoryIntegrationTest`. Persistence Auditor: achados não-bloqueantes
(`sport_id` como coluna não-líder do índice composto — aceitável pra tabela de baixo volume por
tenant; `ADD COLUMN NOT NULL` sem `DEFAULT` falharia se algum tenant real já tivesse `dim_team`
populada antes desta migration — consistente com a premissa do plano de que a tabela nunca foi
usada em tenant real, sem ação corretiva necessária).

**Achado de processo, corrigido durante o fechamento**: o PR `feature/SV-299 -> develop` falhou o
Quality Gate do SonarCloud na primeira tentativa (`B Reliability Rating on New Code`) num arquivo
que `feat-013` nunca tocou (`EquityCurveCalculator.java`, de `feat-012`) — o projeto usa janela de
"New Code" por tempo (não por diff de PR), então código de horas atrás ainda conta como novo.
Corrigido (`n - 1` de `int` pra `long` explícito antes de `BigDecimal.valueOf`, evita overflow
teórico antes do widening implícito) mesmo fora do escopo nominal de `feat-013`, porque bloqueava
o gate de merge — registrado aqui em vez de escondido no commit.

3 PRs: #39 (subtask `feat-013.3`→story, CI verde sem Sonar), #40 (story→`develop`, CI+SonarCloud
verdes após o fix acima). `./mvnw -q verify`/`./init.sh` do serviço e da raiz verdes. Libera
`epic-012` (web).

## `feat-012` fechada — GET /api/v1/statistics/search (2026-09-10)

Escopo novo, fora do backlog original do TCC1 (pedido do usuário, `epic-011` da raiz). Endpoint
de decisão pré-aposta, distinto do dashboard consolidado (`feat-006`): `sportId`/`leagueId`
obrigatórios (400 RFC 7807 localizado), `teamId`/`bettingHouseId`/`marketId`/`tipsterId`/`from`/`to`
opcionais. Resposta: `summary` (ROI, taxa de acerto, odd média, drawdown máximo, Índice de Sharpe
simplificado) + `timeline` (equity curve). Fórmulas em `docs/STATISTICS.md` (nota nova).

Duas mudanças de schema (migration aditiva, nullable): `DIM_TEAM` nova (chave natural por nome —
`team1`/`team2` não têm catálogo em `bets-service`) e coluna `odd` em `FACT_BET` (já trafegava no
evento desde o início, nunca tinha sido persistida). `EquityCurveCalculator` (domain, puro, sem
I/O) calcula `maxDrawdown`/`sharpeRatio` a partir da série ordenada — testado com valores
conferidos à mão, não só "não lança exceção".

**Achado real corrigido antes do código** (Plan Reviewer): `processSettled` recalculava `dateId`
a partir de `settledAt` em vez de preservar o `dateId` já resolvido de `betDate` (data do JOGO,
decisão do usuário) por `processCreated` — corrompia silenciosamente o agregado mensal do
dashboard consolidado (`feat-006`) para toda aposta liquidada em mês diferente do jogo, não só a
série nova desta feature. Corrigido preservando o `dateId` existente; residual documentado
(`BetSettled` fora de ordem sem `betDate` no payload).

`Delivery Reviewer`: PASS (sem regressão em `GET /api/v1/statistics`). `Test Suite Auditor`: PASS.
`Persistence Auditor`: CONCERNS, 1 P2 aceito (corrida check-then-act em
`DimensionResolver.resolveTeam`, mesmo padrão das outras 5 dimensões, auto-recuperável via
retry/DLQ já validado em `epic-007`). `./init.sh` do serviço e da raiz verdes. 6 subtasks
(`SV-293`..`298`), branch `feature/SV-292` mergeada em `develop`.

## `feat-009` fechada — aviso do painel Problems do VSCode (2026-09-08)

Achado durante sessão de `api-gateway`: `org.testcontainers.containers.RabbitMQContainer`
deprecado (módulo dedicado `org.testcontainers.rabbitmq`, já dependência do `pom.xml`, mesmo
construtor — mesma migração que `PostgreSQLContainer` já tinha feito). Zero mudança de
comportamento — `Delivery Reviewer` (passe próprio): PASS, `grep` confirmou zero referência
remanescente ao pacote antigo. `./init.sh` verde. PR `feature/SV-174` → `develop`, CI/SonarCloud
verde.

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

- [x] **`feat-003` (Consumo dos eventos) — `done` em 2026-09-07.** Primeiro consumo real de
      evento com efeito persistente: `BetCreated` insere `FACT_BET` `pending`, `BetSettled` faz
      upsert real (`FactBetJpaEntity.applyFrom`, carrega a instância rastreada em vez de
      construir nova), idempotência real via `PROCESSED_EVENT` (`existsByEventId` + insert na
      mesma transação), mensagens fora de ordem tratadas sem reverter liquidação já aplicada
      (`BetCreated` vira no-op se `FACT_BET` já existe), tenant não resolvível vai direto pra
      DLQ. `DimensionResolver` resolve/cria as 6 dimensões (5 por id, `DimDate` por chave
      natural). Story SV-129, 3 subtasks (SV-130..132). Evidência completa em
      `feature_list.json`.

- [x] **`feat-004` (RF09 — cálculo de métricas) — `done` em 2026-09-07.** Primeiro cálculo de
      métrica de negócio deste serviço: `FactBetRepository` ganha 4 métodos de agregação bruta
      via JPQL (overall + segmentado por sport/market/betting-house), filtrando `status <>
      pending` (RN06 — equivalente por o enum só ter 4 valores) e agrupando com join explícito
      por condição (dimensões não têm `@ManyToOne`, colunas UUID simples). `CalculateMetricsService`
      transforma o agregado em ROI/taxa de acerto (RN04/RN09) — divisão por zero retorna
      `BigDecimal.ZERO`, não exceção. Sem endpoint HTTP (`feat-006`) nem cache (`feat-005`) ainda.
      Story SV-133, 3 subtasks (SV-134..136). Evidência completa em `feature_list.json`.

- [x] **`feat-005` (Cache Redis cache-aside) — `done` em 2026-09-07.** Primeiro cache deste
      serviço (e do projeto): `MetricsCacheRepository`/`RedisMetricsCacheRepository` — nenhum
      método recebe o tenant como parâmetro, o adapter resolve o slug via `TenantContextHolder`,
      mesma simetria que `TenantIdentifierResolver` já usa pro schema do Hibernate.
      `FactBetRepository.aggregateByMonth()` estende o padrão de join explícito por condição
      (feat-004) pra `DimDate.year/month`. `GetDashboardMetricsService` orquestra cache-aside
      pras 5 chaves (`dashboard:consolidated`, `segment:{sport|market|house}`,
      `stats:monthly:{year}_{month}`) — hit responde direto, miss calcula via
      `CalculateMetricsUseCase` e grava. Invalidação real no consumo do evento: **só
      `BetSettled` evicta** — `BetCreated` nunca evicta porque RN06 exclui `pending` de toda
      agregação, então aquele insert é invisível pras métricas cacheadas (achado real, corrige a
      premissa do plano original). TTL de segurança de 1h em toda gravação (rede de segurança,
      não regra de negócio). Story SV-137, 3 subtasks (SV-138..140). Evidência completa em
      `feature_list.json`.

- [x] **`feat-006` (RF11 — `GET /api/v1/statistics` com filtros dinâmicos) — `done` em
      2026-09-07.** 7 filtros opcionais (`bettingHouseId`/`sportId`/`leagueId`/`marketId`/
      `tipsterId`/`from`/`to`), resposta em **bundle único** (`StatisticsDashboard`: overall +
      3 segmentos + série mensal) — decisão do usuário (2026-09-07, `AskUserQuestion`) entre
      bundle único, resposta mínima com `groupBy` e endpoints separados por segmento.
      `StatisticsFilter` substituiu as assinaturas sem parâmetro de `feat-004`/`feat-005` (uma
      mecânica só). Sem nenhum filtro usa o cache-aside de `feat-005`; qualquer filtro presente
      bypassa o cache e calcula direto. Dois achados reais corrigidos: Postgres não infere o tipo
      de um parâmetro `null` usado só dentro de `CAST`/`FUNCTION` (limites-sentinela em vez de
      outro `IS NULL OR`); SonarCloud `java:S107` (mais de 7 parâmetros nos métodos de agregação
      filtrada, consolidados num único parâmetro via SpEL — `ResolvedStatisticsFilter`). Story
      SV-141, 3 subtasks (SV-142..144). Evidência completa em `feature_list.json`.

- [x] **`feat-007` (Pipeline de CI) — `done` em 2026-09-07.** Fechamento formal do backlog deste
      serviço — sem código/workflow novo. O pipeline real (6 passos: CHANGELOG, i18n, build,
      testes+cobertura, SonarCloud, gate de zero issues) já rodava em produção desde `feat-001.1`
      e gateou com sucesso todas as PRs de `feat-001..006`. Único achado real: a `description` da
      própria feature estava desatualizada (citava 5 passos e comandos Maven antigos) — corrigida
      para bater com o `ci.yml` real. Achado tardio de planejamento: esta feature não estava no
      escopo inicialmente previsto para fechar o backlog do serviço, só percebida ao reler o
      `feature_list.json` completo antes de declarar o epic concluído. Story SV-145, 1 subtask
      (SV-146). Evidência completa em `feature_list.json`.

### Em andamento

- Nenhuma feature em andamento. **Backlog do serviço esgotado** (`feat-001`..`feat-007` `done`) —
  fecha `epic-004` da raiz.

### Próximos passos (Next Steps)

Nenhum — não há mais features planejadas para este serviço. Eventual trabalho futuro (nova
feature de negócio) exigiria uma nova entrada em `feature_list.json` antes de começar.

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

## Arquivos modificados nesta sessão (`feat-003`)

- `application/{DimensionResolver,ProcessBetEventService}.java`.
- `domain/port/in/{BetCreatedEvent,BetSettledEvent,ProcessBetEventUseCase}.java`.
- `adapter/in/messaging/BetEventListener.java` (reescrito — parsing de payload + delegação ao
  use case, em vez de log.info).
- `adapter/out/persistence/{FactBetJpaEntity,JpaFactBetRepository}.java` (upsert real via
  `applyFrom`, não reconstrói a entidade).
- Testes: `BetEventListenerIntegrationTest` (reescrito, `TenantSchemaIntegrationSupport`),
  `ProcessBetEventServiceTest`, `DimensionResolverTest`.
- `feature_list.json`, `CHANGELOG.md` deste serviço.

## Evidência de conclusão (`feat-003`)

- `./init.sh` (`mvn verify`) verde com Docker ativo — 66 testes.
- CI verde nas 2 PRs de subtask e na PR de story→develop (#17), incluindo SonarCloud.
- Detalhe completo no campo `evidence` de `feat-003` em `feature_list.json`.

## Arquivos modificados nesta sessão (`feat-004`)

- `domain/model/{BetAggregate,SegmentedBetAggregate,BetMetrics,SegmentedBetMetrics}.java`.
- `domain/port/out/FactBetRepository.java` (4 métodos de agregação), `domain/port/in/
  CalculateMetricsUseCase.java`.
- `adapter/out/persistence/{AggregateProjection,SegmentedAggregateProjection,
  FactBetSpringDataRepository,JpaFactBetRepository}.java`.
- `application/CalculateMetricsService.java`.
- Testes: `FactBetAggregationIntegrationTest` (novo), `CalculateMetricsServiceTest` (novo,
  parametrizado).
- `feature_list.json`, `CHANGELOG.md` deste serviço; `../../docs/services/stats-service.md`
  (repositório raiz) documentando o mecanismo e a decisão de `settledCount` incluir `void`.

## Evidência de conclusão (`feat-004`)

- `./init.sh` (`mvn verify`) verde com Docker ativo.
- CI verde nas 3 PRs de subtask e na PR de story→develop (#21), incluindo SonarCloud (sem
  achados).
- Achado real do self-review (Delivery Reviewer/Test Suite Auditor): teste de agregação não
  cobria `void` explicitamente (RN06 o inclui ao lado de `won`/`lost`) — corrigido antes do
  fechamento, sem defeito de produção encontrado.
- Detalhe completo no campo `evidence` de `feat-004` em `feature_list.json`.

## Arquivos modificados nesta sessão (`feat-005`)

- `domain/model/{MonthlyBetAggregate,MonthlyBetMetrics}.java`, `domain/port/out/
  MetricsCacheRepository.java`, `domain/port/in/GetDashboardMetricsUseCase.java`.
- `adapter/out/cache/RedisMetricsCacheRepository.java` (novo pacote), `adapter/out/persistence/
  MonthlyAggregateProjection.java`, `FactBetSpringDataRepository`/`JpaFactBetRepository`
  (método `aggregateByMonth`).
- `application/{CalculateMetricsService,GetDashboardMetricsService,ProcessBetEventService}.java`.
- `TestcontainersConfiguration` (bean `RedisContainer`), `application.yml`/`.env.example`
  (`REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`), `pom.xml` (`spring-boot-starter-data-redis`,
  `com.redis:testcontainers-redis`).
- Testes: `RedisMetricsCacheRepositoryIntegrationTest`, `CacheInvalidationOnEventIntegrationTest`,
  `GetDashboardMetricsServiceTest` (novos), `ProcessBetEventServiceTest` atualizado.
- `feature_list.json`, `CHANGELOG.md` deste serviço; `../../docs/services/stats-service.md`
  (repositório raiz) documentando o mecanismo completo de cache.

## Evidência de conclusão (`feat-005`)

- `./init.sh` (`mvn verify`) verde com Docker ativo.
- CI verde nas 3 PRs de subtask e na PR de story→develop (#25), incluindo SonarCloud.
- 2 achados reais corrigidos antes do fechamento: (1) self-review — mês sem nenhuma aposta
  liquidada nunca ficava em cache (`GROUP BY` não retorna grupo vazio), forçando recomputo da
  série inteira a cada consulta a um mês vazio; (2) SonarCloud `java:S1192` — literais de chave
  (`"sport"`/`"market"`/`"house"`/`"tenant:"`) duplicados, extraídos para constantes.
- Detalhe completo no campo `evidence` de `feat-005` em `feature_list.json`.

## Arquivos modificados nesta sessão (`feat-006`)

- `domain/model/{StatisticsFilter,StatisticsDashboard}.java`, `domain/port/in/
  GetStatisticsDashboardUseCase.java`.
- `domain/port/out/FactBetRepository.java`/`domain/port/in/CalculateMetricsUseCase.java`
  (assinaturas migradas pra aceitar `StatisticsFilter`).
- `adapter/out/persistence/{ResolvedStatisticsFilter,FactBetSpringDataRepository,
  JpaFactBetRepository}.java` (predicados opcionais + SpEL, limites-sentinela pra `from`/`to`).
- `application/{CalculateMetricsService,GetDashboardMetricsService,
  GetStatisticsDashboardService}.java`.
- `adapter/in/web/StatisticsController.java` (novo, `GET /api/v1/statistics`).
- Testes: `StatisticsControllerIntegrationTest` (novo, HTTP real com parse de JSON),
  `FactBetAggregationIntegrationTest`/`CalculateMetricsServiceTest`/
  `GetDashboardMetricsServiceTest` atualizados pras novas assinaturas.
- `feature_list.json`, `CHANGELOG.md` deste serviço; `../../docs/API-CONTRACTS.md` (exemplo do
  payload de resposta), `../../docs/services/{web,stats-service}.md` (repositório raiz —
  correção do drift de query params em `web.md`, nota de fechamento em `stats-service.md`).

## Evidência de conclusão (`feat-006`)

- `./init.sh` (`mvn verify`) verde com Docker ativo.
- CI verde nas 3 PRs de subtask e na PR de story→develop (#29 — 1 rerun por flake de timing do
  Awaitility num teste pré-existente não relacionado, confirmado não recorrente), incluindo
  SonarCloud (`java:S107`, métodos com mais de 7 parâmetros, corrigido antes do merge final).
  Achado real separado, não do SonarCloud: erro de inferência de tipo do Postgres para parâmetro
  `null` dentro de `CAST`/`FUNCTION`, corrigido com limites-sentinela (ver acima).
- Detalhe completo no campo `evidence` de `feat-006` em `feature_list.json`.

## Evidência de conclusão (`feat-007`)

- `./init.sh` verde — nenhuma mudança de código.
- CI verde na PR de subtask e na PR de story→develop (#31), incluindo SonarCloud.
- Detalhe completo no campo `evidence` de `feat-007` em `feature_list.json`.

## Notas para a próxima sessão

Nenhuma feature pendente. `epic-004` (stats-service) da raiz está `done` — próxima sessão que
tocar este serviço deve começar por decidir com o usuário qual nova feature de negócio (fora do
backlog original) entra no `feature_list.json` antes de qualquer código.

## `feat-010` — retry de aplicação para RabbitMQ 4.3+ (2026-09-10)

Achado real de `infra/feat-002` (teste de resiliência de `epic-007`, outro repositório), contra o
broker de verdade (`rabbitmq:4-management-alpine`, 4.3.5): a partir do RabbitMQ 4.3,
`nack(requeue=true)` — comportamento padrão do `ConditionalRejectingErrorHandler` do Spring AMQP
em qualquer falha de listener não-fatal — deixou de contar para `x-delivery-limit` da fila quorum
`stats.bet-events`. Reproduzido ao vivo: derrubar `postgres-stats` fez `BetEventListener.onMessage`
falhar 15 vezes seguidas sem a mensagem nunca cair na DLQ (`x-delivery-count` travado em 1). Isso
quebrava o próprio objetivo de isolamento de falha do `epic-007` — mensagem envenenada travaria o
único consumidor para sempre.

Fix: mover a contagem de tentativas do broker (que não dispara mais nesse cenário) para a
aplicação. `org.springframework.retry:spring-retry` adicionado como dependência direta (não vem
transitivo de `spring-boot-starter-amqp`); `spring.rabbitmq.listener.simple.retry` (`enabled`,
`max-attempts: 3`, `initial-interval: 1000`) no bloco DEFAULT de `application.yml` — achado MAJOR
do Plan Reviewer: colocar isso só em `dev`/`prod` deixaria o teste de integração (perfil `test`)
validar com o mecanismo desligado, dando falso-positivo. Após esgotar as 3 tentativas *em
processo* (sem tocar o broker entre elas), o `RejectAndDontRequeueRecoverer` padrão rejeita com
`requeue=false`, que sempre morta-letra via DLX independente da contagem do broker.

`BetEventListenerRetryIntegrationTest` novo (contexto Spring próprio, bean `@Primary` decorando
`ProcessBetEventUseCase` sempre lançando `RuntimeException`) prova o caminho "falha transitória
esgota tentativas → DLQ" — os 2 testes de DLQ existentes cobriam só o caminho de reject imediato
por erro de dado (`AmqpRejectAndDontRequeueException`, tenant não resolvível/schema inválido, sem
retry cabível), não uma falha transitória real. Esse caminho não foi tocado.

Achado de processo: as 4 subtasks (SV-269..272) tinham sido fechadas e mescladas localmente numa
sessão anterior sem nunca passar por PR/CI real do GitHub. Corrigido nesta sessão — branches
empurradas, PR `feature/SV-268 -> develop` passou pela CI real (build+testes+SonarCloud) antes do
merge, desvio documentado na descrição do PR em vez de escondido.

O fix foi consumido e confirmado de verdade em `infra/feat-002.4` (mesma sessão): o cenário de DLQ
daquele teste rodou contra esta versão já corrigida e a mensagem morta-letrou corretamente após
~105s (tempo dominado pelo `connection-timeout` de 30s do HikariCP contra o Postgres caído, não
pelo `initial-interval` de 1s entre tentativas).

`./mvnw -q verify`/`./init.sh` verdes. `docs/DECISIONS-LOG.md`/`docs/API-CONTRACTS.md`/
`docs/services/infra.md` (raiz) atualizados no mesmo commit lógico com o achado completo.

## `feat-011` — Dockerfile para imagem de produção (2026-09-10)

Achado real de `infra/feat-004` (migração para Kubernetes, `epic-010` da raiz): este serviço
nunca teve `Dockerfile` próprio. Multi-stage idêntico ao padrão de `auth-service feat-011`
(build `eclipse-temurin:25-jdk-alpine`, runtime `25-jre-alpine`, usuário não-root, porta 8083).
Build real e execução real testados contra a infra (`postgres-stats`, `rabbitmq`, `redis`):
`/actuator/health` UP. Imagem usada de fato pelos manifests Kubernetes de `infra/feat-004`. 1
subtask (SV-281, story SV-280), 2 PRs (#37 subtask->feature, #38 feature->develop), CI+SonarCloud
verdes nos dois.

## `feat-015` fechada — extensão do dashboard consolidado: PRE/LIVE, avgOdd, won/lost/void, byBetType (2026-09-11)

Fecha `epic-014` da raiz (escopo novo, fora do backlog original do TCC1, pedido do usuário
2026-09-10). `GET /api/v1/statistics` ganha `wonCount`/`lostCount`/`voidCount`/`preCount`/
`liveCount`/`avgOdd` em `overall`/`bySport`/`byMarket`/`byBettingHouse`/`monthly`, mais um 6º
segmento `byBetType` (2 buckets fixos `PRE`/`LIVE`). Contrato já escrito antes do código em
`docs/API-CONTRACTS.md`/`docs/STATISTICS.md`/`docs/services/stats-service.md` (sessão de
planejamento anterior) — esta feature foi puramente implementação contra um design já fechado.

4 subtasks (story SV-343): `feat-015.1` (SV-344, `FACT_BET.betType` persistido — gravado só no
*insert* de `BetCreated`, preservado no *upsert* de `BetSettled` porque aquele payload não carrega
`betType`, mesmo padrão exato já usado para `dateId`; enum `BetType` com `AttributeConverter`
dedicado, mesmo padrão de `BetStatusAttributeConverter`), `feat-015.2` (SV-345, campos novos nas 5
queries JPQL existentes), `feat-015.3` (SV-346, segmento `byBetType` — `dimensionId` migrado de
`UUID` para `String` em `SegmentedBetAggregate`/`SegmentedBetMetrics`, único segmento sem uuid de
catálogo por trás), `feat-015.4` (SV-347, fechamento formal).

**3 achados MAJOR do Plan Reviewer, corrigidos antes do código** (não exigiram decisão do
usuário): (1) toda comparação de enum nas queries JPQL deve usar `@Param` tipado, nunca literal de
string solto (`f.status = 'LOST'`) — o codebase já evitava esse padrão desde `feat-006`
(`:pending`), aqui só ficou explícito o porquê: literal de string arriscaria o
`AttributeConverter` não ser aplicado de forma garantida. (2) projeção de `byBetType` expõe o
getter no tipo real do enum (`BetTypeAggregateProjection.getBetType(): BetType`), conversão pra
`String` feita explicitamente no adapter (`JpaFactBetRepository`), não implícita numa projeção
Spring Data. (3) mudança de tipo `dimensionId` exigiu atualizar 5 arquivos de teste que já
referenciavam `UUID` — listado explicitamente no plano pra não subestimar o escopo.

**Achado real do Delivery Reviewer** (self-review, sem subagentes — independência reduzida,
declarada, mesmo padrão já usado nas demais features deste serviço): o exemplo JSON de
`byBetType` em `docs/API-CONTRACTS.md` (escrito na sessão de planejamento, antes do código) não
mostrava `preCount`/`liveCount` nos itens do segmento — a decisão de implementação (reaproveitar o
mesmo `record` `BetMetrics` dos outros 5 segmentos em vez de um tipo apartado, já confirmada no
`plan_review`) inclui esses 2 campos ali também, ainda que triviais dentro do próprio bucket
(`PRE` sempre tem `preCount == settledCount`). Doc corrigido pra bater com a implementação real,
no mesmo commit de fechamento.

**Achado do self-review durante a implementação, refutado com evidência** (não virou subtask): a
preocupação de que estender `BetMetrics` quebraria a deserialização de entradas já cacheadas no
Redis de antes do deploy não se confirmou — Jackson 3 (`tools.jackson`, autoconfiguração padrão do
Spring Boot 4, sem override de estrita neste repositório) preenche componente de `record` ausente
no JSON com o *default* do tipo (`0`/`0L`, `null`) em vez de lançar exceção. Risco residual aceito
(até 1h de TTL com campos novos zerados/nulos num cache pré-deploy), documentado mas não corrigido
— não é um problema introduzido por esta feature, é comportamento pré-existente do framework.

`Delivery Reviewer`/`Test Suite Auditor`/`Persistence Auditor` (passe próprio, sem subagentes —
independência reduzida, declarada) rodados contra o diff completo (37 arquivos): todos `PASS`.
`./init.sh` (`mvn verify`, JaCoCo 80%) verde localmente com Docker ativo em cada uma das 4
subtasks. CI+SonarCloud verdes nas 4 PRs de subtask e na PR `feature/SV-343 -> develop` (#50).
`docs/API-CONTRACTS.md`/`docs/services/stats-service.md` (repositório raiz) atualizados no commit
de fechamento. Fecha `epic-014` da raiz.

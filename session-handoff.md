# Session Handoff — stats-service

## Current Objective

- `epic-004` e `epic-014` (raiz) `done`. Nenhuma feature em andamento neste serviço.
- Branch / commit: `develop` @ `d9112c8` (merge de `feature/SV-343`, fecha `feat-015`).

## Completed This Session (2026-09-11)

- [x] **`feat-015` fechada** (extensão do dashboard consolidado, story SV-343, 4 subtasks
      SV-344..347) — `GET /api/v1/statistics` ganha `wonCount`/`lostCount`/`voidCount`/
      `preCount`/`liveCount`/`avgOdd` em `overall`/`bySport`/`byMarket`/`byBettingHouse`/
      `monthly`, mais 6º segmento `byBetType` (2 buckets fixos `PRE`/`LIVE`). `FACT_BET.betType`
      persistido (insert-only em `BetCreated`, preservado no upsert de `BetSettled`). Fecha
      `epic-014` da raiz. Ver `progress.md` para o detalhe completo (3 achados MAJOR do Plan
      Reviewer, 1 achado real do Delivery Reviewer — doc drift em `docs/API-CONTRACTS.md`
      corrigido).

## Verification Evidence

| Check | Command | Result | Notes |
|---|---|---|---|
| `./init.sh` | `mvn verify`, JaCoCo 80% | pass | Verde em cada uma das 4 subtasks. |
| CI+SonarCloud | GitHub Actions | pass | 4 PRs de subtask + PR `feature/SV-343 -> develop` (#50), todos verdes. |
| Delivery/Test Suite/Persistence Auditor | passe próprio, sem subagentes | PASS/PASS/PASS | Achado real corrigido: doc `byBetType` sem `preCount`/`liveCount`. |

## Blockers / Risks

Nenhum.

## Next Session Startup

1. Ler `../../CLAUDE.md` e o `CLAUDE.md` deste serviço.
2. Rodar `./init.sh` (deve passar).
3. Backlog deste serviço esgotado (`feat-001`..`feat-015` `done`). Próximos epics elegíveis da
   raiz sobre `stats-service`: `epic-016` (quebra diária, `GET /api/v1/statistics/daily`) e
   `epic-018` (segmentos `byLeague`/`byTipster`) — ambos só dependem de `epic-004` (`done`),
   podem avançar em qualquer ordem. Ver `../../feature_list.json` para a descrição completa e
   `../../session-handoff.md` (raiz) pro racional de escolha entre os epics elegíveis de todos os
   serviços.

## Recommended Next Step

Nenhuma feature de negócio pendente neste serviço até que o usuário/raiz decida qual dos epics
elegíveis (`epic-016`/`epic-018`) entra em seguida — criar a feature granular (`feat-016`) em
`feature_list.json` deste serviço com Plan Reviewer antes de codificar.

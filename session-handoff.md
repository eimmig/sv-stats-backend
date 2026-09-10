# Session Handoff — stats-service

## Current Objective

- `epic-004` (raiz) `done`. `feat-013` (addendum sobre `epic-011`) fechada nesta sessão. Nenhuma
  feature pendente neste harness.
- Branch / commit: `develop` @ merge de `feature/SV-299` (PR #40).

## Completed This Session (2026-09-10)

- [x] **`feat-013` fechada** (story SV-299, subtasks SV-300..302, PRs #39 subtask->feature + #40
      feature->develop): `DIM_TEAM` ganha `sportId` (FK `DIM_SPORT`, NOT NULL), chave natural vira
      `(name, sportId)`; `GET /api/v1/statistics/teams?sportId=<uuid>` novo (autocomplete escopado
      por esporte da tela "Buscar Estatísticas" em `apps/web`).
- [x] Achado real do Test Suite Auditor corrigido: teste novo prova a `UNIQUE(name, sport_id)` no
      banco (`shouldRejectDuplicateNameAndSportAtTheDatabaseLevel`), não só o caminho de aplicação.
- [x] Achado de gate corrigido: SonarCloud Quality Gate falhou no PR story->develop por um
      Reliability finding em `EquityCurveCalculator.java` (código de `feat-012`, capturado pela
      janela de "New Code" por tempo do projeto, não por diff do PR) — `n - 1` casteado pra `long`
      explicitamente antes de `BigDecimal.valueOf`.

## Verification Evidence

| Check | Command | Result | Notes |
|---|---|---|---|
| Build/test | `./mvnw -q verify` | pass | JaCoCo gate incluso |
| Local harness | `./init.sh` | pass | |
| CI (full gate) | GitHub Actions + SonarCloud | pass | PR #40, após fix do achado de Reliability |
| Delivery Reviewer | — | PASS | 1 desvio de plano aceito (endpoint no controller existente) |
| Test Suite Auditor | — | CONCERNS -> corrigido | achado P2 (constraint sem teste DB-level) corrigido antes de fechar |
| Persistence Auditor | — | CONCERNS (não-bloqueante) | achados P2/P3 documentados, consistentes com o plano |

## Blockers / Risks

- Nenhum.

## Next Session Startup

1. Ler `../../CLAUDE.md` e o `CLAUDE.md` deste serviço.
2. `feature_list.json` deste harness: todas as features `done` (`feat-001..013`). Nenhum trabalho
   pendente aqui até surgir novo achado cross-service ou nova feature.
3. Rodar `./init.sh` (deve passar).

## Recommended Next Step

- Nenhum próximo passo pendente neste harness. `epic-012` (raiz, `apps/web` — tela "Buscar
  Estatísticas") está liberado, consumindo tanto `feat-012` (`GET /api/v1/statistics/search`)
  quanto `feat-013` (`GET /api/v1/statistics/teams`).
- Projeto SonarCloud deste serviço usa janela de "New Code" por tempo, não por diff de PR — um PR
  sem nenhuma linha tocada num arquivo ainda pode falhar o gate por código de horas/dias atrás
  entrando na janela. Vale considerar se isso é intencional (config do projeto no SonarCloud) ou
  se devia ser "Reference branch" — não investigado nesta sessão, sinalizado aqui.

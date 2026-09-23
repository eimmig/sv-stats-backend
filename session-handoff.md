# Session Handoff — stats-service

> Estado atual, não histórico. O diário cronológico é o `progress.md` — este arquivo é reescrito
> a cada sessão para responder "o que a próxima sessão precisa saber agora".

**Última atualização:** 2026-09-22

## Objetivo atual

`feat-001`..`feat-020` `done` (`feat-018` — alinhar `DIM_TEAM` ao catálogo `TEAM` — fechada em
sessão anterior via `epic-024`; `feat-020`, companion de `bets-service feat-019`, fechada nesta
sessão). Backlog deste serviço esgotado — nenhuma feature `not-started` elegível agora.

## Concluído em 2026-09-22

- [x] **`feat-020` fechada** — `ProcessBetEventService.processCreated` aceita reprocessar
      `FACT_BET` quando a linha existente ainda está `pending`, companion cross-service de
      `bets-service feat-019` (`PUT /api/v1/bets/{id}`). Ver `services/bets-service/progress.md`
      para o desenho completo (decidido no Plan Reviewer daquele serviço). Sem story/PR formal
      nesta sessão (commit direto, fluxo de pareamento). `./init.sh` verde, 157/157 testes.

## Concluído nesta sessão (2026-09-15)

- [x] **`feat-019` fechada** (CD automático — job `deploy` em `ci.yml`, `kubectl rollout restart
      deployment/stats-service` contra `KUBE_CONFIG`/`ci-deployer` de `infra/feat-007`).
      Reaproveitou byte a byte o padrão já revisado em `bets-service feat-018` na mesma sessão
      (mesmas 2 correções MINOR do Plan Reviewer: sem `azure/setup-kubectl`, `permissions: {}`
      explícito) — única diferença é o nome do `Deployment` (`stats-service`). Story SV-426,
      subtasks SV-427/SV-428, PRs #59/#60/#61, CI+SonarCloud verdes, merge
      `feature/SV-426 -> develop` concluído.
- [x] **Disparo real do job adiado deliberadamente** (mesma decisão de `bets-service feat-018`):
      `main` deste repositório estava ~20 commits atrás de `develop` (`feat-014`..`feat-017`
      acumulados, nenhuma promoção `develop -> main` ainda). Promover agora só para observar o
      job `deploy` rodar de verdade seria uma decisão de release mais ampla, não exclusiva desta
      feature — adiado, não forçado. **Achado de processo nesta sessão**: um `git merge --no-ff`
      local foi tentado por engano em vez de abrir PR pro gate pesado (`story -> develop`) —
      revertido (`git reset --hard origin/develop`, nada tinha sido empurrado ainda) e refeito
      corretamente via `gh pr create`/`gh pr merge` (PR #61, CI+SonarCloud reais). Lição: mesmo
      reaproveitando um padrão já validado, o merge pro gate pesado sempre passa por PR real no
      GitHub, nunca merge local direto — só o merge subtask->story permite o atalho de
      `git merge --no-ff` local quando não há PR aberto (não é o caso aqui, havia PR).

## Bloqueios / Riscos

Nenhum bloqueio. Mesmo risco documentado em `bets-service` (não exclusivo daqui): a promoção
`develop -> main` deste repositório ainda não aconteceu — quando acontecer, é a primeira execução
real do job `deploy`; registrar a confirmação (log do Actions) em `docs/services/infra.md`.

## Próxima sessão — por onde começar

1. Rodar `./init.sh` (precisa de Docker rodando, Testcontainers).
2. Backlog deste serviço sem feature elegível — `feat-018` é `BLOCKED` (ver acima). Trabalhar
   noutro harness (WIP máximo 1 por serviço) — ver `feature_list.json` da raiz.
3. Se `feat-018` for retomada: reler o `plan_review` já escrito nela antes de popular subtasks.

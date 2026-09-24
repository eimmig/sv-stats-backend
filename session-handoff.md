# Session Handoff — stats-service

> Estado atual, não histórico. O diário cronológico é o `progress.md` — este arquivo é reescrito
> a cada sessão para responder "o que a próxima sessão precisa saber agora".

**Última atualização:** 2026-09-24

## Objetivo atual

`feat-001`..`feat-025` `done`. Backlog deste serviço esgotado — nenhuma feature `not-started`
elegível agora.

## Concluído nesta sessão (2026-09-24)

- [x] **`feat-025` fechada** — segmento `byTeam` em `GET /api/v1/statistics` (`epic-036` da raiz,
      aposta conta para os 2 times). Story SV-641. Consumidor: `web feat-047`.

- [x] **`feat-024` fechada** — filtro `betType` em `/statistics/search` (`epic-035` da raiz).
      Story SV-607. Consumidor: `web feat-038`.
- [x] **`feat-023` fechada** — `LocalizedRuntimeException` extraída (3º harness do `epic-034` da
      raiz). Story SV-588, PRs #75-#77, CI+SonarCloud verdes. Detalhe em `progress.md` e na
      `evidence` da feature.

## Bloqueios / Riscos

Job `deploy` (`epic-028`) segue falhando em `main` porque o `KUBE_CONFIG` não alcança o cluster a
partir de runners hospedados — usuário decidiu em 2026-09-23 aceitar esse ruído (falha isolada, não
bloqueia `release`/demais jobs); não é mais bloqueio para promover `develop -> main`. Ver
`session-handoff.md` da raiz e `docs/services/infra.md`.

## Próxima sessão — por onde começar

1. Rodar `./init.sh` (precisa de Docker rodando, Testcontainers).
2. Backlog deste serviço vazio.
3. Toda exceção localizada nova estende `LocalizedRuntimeException` (ver `docs/convencoes.md` da
   raiz).

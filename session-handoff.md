# Session Handoff — stats-service

> Estado atual, não histórico. O diário cronológico é o `progress.md` — este arquivo é reescrito
> a cada sessão para responder "o que a próxima sessão precisa saber agora".

**Última atualização:** 2026-09-23

## Objetivo atual

`feat-001`..`feat-021` `done`. Backlog deste serviço esgotado — nenhuma feature `not-started`
elegível agora.

## Concluído nesta sessão (2026-09-23)

- [x] **`feat-021` fechada** (Reformulação de marca StakeVault -> Arka, continuação do `epic-032`
      da raiz - 3º dos 4 serviços Java, mesmo plano base de `auth-service feat-019`). Único ponto
      real de marca: `pom.xml` linha 15 (`<description>`). Plan Reviewer condensado (READY) +
      Delivery Reviewer (PASS). Story SV-555, PRs #69-71, CI+SonarCloud verdes.
- [x] Mesmo achado de processo do companion `bets-service feat-020`: `develop` tinha 1 commit
      local não publicado (`feat-020`, companion de `bets-service feat-019`) - sincronizado antes
      de ramificar.
- [x] Mesmo residual de ambiente (processos `java.exe` órfãos travando o `repackage` local do
      Maven no Windows, mesmo lock nos 4 serviços Java) documentado em
      `services/auth-service/progress.md` - `mvn test` local verde, `mvn verify` completo
      confirmado pelo CI (Linux).

## Bloqueios / Riscos

Mesmo risco documentado em `bets-service` (não exclusivo daqui): a promoção `develop -> main`
deste repositório ainda não aconteceu — quando acontecer, é a primeira execução real do job
`deploy`, mas o `KUBE_CONFIG` de `epic-028` não alcança o cluster a partir de runners hospedados
(ver `services/bets-service/session-handoff.md` e `docs/services/infra.md` pro detalhe) —
promoção pausada até o usuário decidir o caminho de rede.

## Próxima sessão — por onde começar

1. Rodar `./init.sh` (precisa de Docker rodando, Testcontainers).
2. Backlog deste serviço vazio.
3. **Não promover `develop -> main`** até o usuário decidir o caminho de rede do `KUBE_CONFIG`.

# Session Handoff — stats-service

## Current Objective

- `epic-004` (raiz) `done`. `epic-014` (raiz, extensão do dashboard consolidado) `in-progress` —
  claimed nesta sessão, ainda sem feature granular criada aqui (próximo passo).
- Branch / commit: `develop` @ `8582fef` (close de `feat-014`, Docker/GHCR).

## Completed This Session (2026-09-10)

- [x] **`feat-014` fechada** (Docker/GHCR publish, story SV-328) — encontrada `in-progress` de
      uma sessão anterior (subtasks já `done`, mas validação real nunca tinha ocorrido) enquanto
      tentando iniciar `epic-014` da raiz aqui — WIP máximo 1 por serviço bloqueava. Fechado:
      primeiro merge `develop`→`main` deste serviço (PR #42, depois #44), imagem confirmada
      publicada em `ghcr.io/eimmig/sv-stats-backend`.
- [x] **Resolvido o "não investigado" da sessão anterior** sobre o SonarCloud usar janela de "New
      Code" por tempo: não era isso. O problema real era a **primeira análise de sempre da branch
      `main`** sem baseline de New Code, retornando `status: "NONE"` (confirmado via API do
      SonarCloud + código-fonte do `sonar-scanner-engine`) que o scanner mal-interpreta como
      `FAILED`. Corrigido ajustando o New Code Definition do projeto no dashboard SonarCloud
      (usuário). Documentado em `docs/CI-CD.md` (raiz) — outros serviços sem merge pra `main`
      ainda podem bater no mesmo problema.

## Verification Evidence

| Check | Command | Result | Notes |
|---|---|---|---|
| `./init.sh` | — | pass | Nenhum código tocado nesta sessão, só CI/CD e docs. |
| CI real em `main` | GitHub Actions | pass | PR #44, `pipeline` + `build-and-push-image` verdes após o fix do SonarCloud. |
| Imagem no GHCR | log do job `build-and-push-image` | confirmado | `ghcr.io/eimmig/sv-stats-backend:latest`+`:<sha>`, digest `sha256:e47e6424...`. |

## Blockers / Risks

Nenhum.

## Next Session Startup

1. Ler `../../CLAUDE.md` e o `CLAUDE.md` deste serviço.
2. Ler `docs/STATISTICS.md` (raiz) antes de codificar `epic-014` — as fórmulas novas
   (wonCount/lostCount/voidCount/preCount/liveCount/avgOdd/byBetType) precisam estar lá antes do
   código, por convenção do harness.
3. Rodar `./init.sh` (deve passar).
4. Criar a feature granular (`feat-015`) em `feature_list.json` deste serviço pra `epic-014` da
   raiz, com Plan Reviewer antes de codificar — ver descrição completa do epic em
   `../../feature_list.json`.

## Recommended Next Step

- **`epic-014` (raiz)**: `GET /api/v1/statistics` ganha `wonCount`/`lostCount`/`voidCount`,
  `preCount`/`liveCount` (a partir de `FACT_BET.betType`, populado só no insert de `BetCreated`),
  `avgOdd`, e o segmento novo `byBetType` (2 buckets fixos PRE/LIVE). Atenção ao risco de colisão
  de coluna já sinalizado na descrição do epic: se `epic-011`/`epic-018` (mesma raiz) também
  tocarem `FACT_BET.odd`, confirmar no Plan Reviewer quem chega primeiro.

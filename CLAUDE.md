# CLAUDE.md — stats-service

Consumidor de eventos, modelo OLAP (esquema estrela) e cache Redis para os dashboards.
Java 25 + Spring Boot 4.x. Parte do harness multinível do monorepo — leia `../../CLAUDE.md`
(raiz) para invariantes cross-service antes deste arquivo, e
`../../docs/services/stats-service.md` para o desenho completo.
Arquitetura interna, build tool, testes e formato de API são normativos e já decididos em
`../../docs/CONVENTIONS.md`, `../../docs/TESTING.md` e `../../docs/API-CONTRACTS.md` — leia-os
antes de `feat-001`.

## Fluxo de início de sessão (Startup Workflow)

1. Confirme o diretório de trabalho (`pwd`) — deve ser `services/stats-service`.
2. Leia `../../CLAUDE.md` e `../../docs/services/stats-service.md`.
3. Rode `./init.sh` para verificar build/testes deste serviço.
4. Leia `feature_list.json` (deste serviço) para a próxima feature granular.
5. Leia `progress.md` (deste serviço).

## Regras específicas deste serviço

- **Uma feature por vez (One feature at a time)**: escolha exatamente uma feature `not-started`
  de `feature_list.json` cujas dependências já estejam `done`.
- **Escopo restrito (stay in scope)**: não edite código de outro serviço a partir desta pasta.
- Banco Postgres próprio (Database per Service), separado do banco OLTP do `bets-service`,
  modelado em **esquema estrela** (Star Schema), isolado por tenant (Schema-per-Tenant,
  `tenant_<slug>`, resolvido a partir do header `X-Tenant-Id` — **não** `X-User-Id`, ver
  `../../docs/API-CONTRACTS.md` e `../../docs/DECISIONS-LOG.md` 2026-08-02). Migração lazy por
  schema (Flyway, ver `../../docs/CONVENTIONS.md` seção "Migrations"). Exponha também uma rota
  administrativa que cria o schema deste serviço quando um tenant novo é provisionado,
  autenticada por `X-Admin-Api-Key` (ver `../../docs/API-CONTRACTS.md`) — chamada manualmente
  pelo operador, **não** por `auth-service` em código (decisão de 2026-08-02, ver
  `../../docs/DECISIONS-LOG.md` item 3: 3 chamadas manuais separadas, este serviço é a última).
- Consome **dois eventos distintos** do RabbitMQ, não um único evento reaproveitado:
  `BetCreated` (registro inicial → *insert* em `FACT_BET`, `status: pending`) e `BetSettled`
  (liquidação → *upsert* na mesma linha por `betId`, `status`/`profit`/`isWin` atualizados) —
  ver `../../docs/API-CONTRACTS.md` e `../../docs/services/stats-service.md`.
- Consumo deve ser **assíncrono e idempotente** — processar a mesma mensagem duas vezes
  (redelivery) não pode duplicar linhas nem métricas. Mecanismo: tabela `PROCESSED_EVENT`
  (verificar `eventId` antes de processar, inserir na mesma transação) — ver
  `../../docs/services/stats-service.md`.
- RN06: agregações (RN04, RN09, dashboards) sempre filtram `FACT_BET` por
  `status IN ('won', 'lost', 'void')` — uma linha com `status: pending` (inserida por
  `BetCreated` mas ainda não liquidada) nunca entra em cálculo de ROI/taxa de acerto.
- Cache Redis segue estritamente o padrão **Cache-Aside** com as chaves documentadas em
  `../../docs/services/stats-service.md` — chaves são `tenant:{tenantId}:...`, **não**
  `user:{id}:...` (corrigido em 2026-08-02: `FACT_BET` é compartilhado por todos os usuários do
  mesmo tenant, ver `../../docs/DECISIONS-LOG.md`). Não introduza outro padrão de cache
  (write-through, write-behind) sem atualizar a nota primeiro.
- Meta de performance (RNF03): dashboard responde em < 300 ms quando o cache está quente.
- **Maven** (não Gradle) e **arquitetura hexagonal** (`domain/`, `application/`, `adapter/`) —
  decisões já tomadas em `../../docs/CONVENTIONS.md`, não reabrir. O consumidor RabbitMQ vive em
  `adapter/in/messaging/`, chamando um `port/in` do domínio.
- Valide toda mensagem consumida contra o JSON Schema correspondente ao `eventType`
  (`../../docs/contracts/bet-created.schema.json` ou `bet-settled.schema.json`) antes de
  processá-la — mensagem que não bate com o schema vai para a DLQ, não é ignorada
  silenciosamente.
- Erros de API em `application/problem+json` (RFC 7807) — ver `../../docs/API-CONTRACTS.md`,
  com `title`/`detail` localizados por `Accept-Language` (`pt-BR`/`en-US`/`es`, ver
  `../../docs/CONVENTIONS.md` seção "Internacionalização (i18n)"). Rota `GET /api/v1/statistics`
  sempre em inglês, independente do idioma da resposta de erro.
- Não escreva no banco OLTP do `bets-service` nem no banco `auth` — este serviço só lê eventos
  do RabbitMQ e escreve no próprio banco OLAP e no Redis.
- **CI/CD (`feat-007`)**: pipeline em `.github/workflows/ci.yml`, **dentro deste repositório**
  (este serviço é seu próprio repositório Git, não um monorepo — ver
  `../../docs/DECISIONS-LOG.md` "Topologia") — changelog, i18n, build, testes, SonarCloud.
  Scripts de validação em `.github/scripts/` (duplicados aqui, não compartilhados com os outros
  serviços). Ver `../../docs/CI-CD.md`. Toda feature adiciona uma entrada em `CHANGELOG.md`
  deste serviço (verificado automaticamente pelo CI quando este repositório existir no GitHub).
- **Skills de agente prioritárias**: `Plan Reviewer` antes de codificar, `Delivery Reviewer` +
  `Test Suite Auditor` + `Persistence Auditor` (banco próprio, schema-per-tenant) antes de
  marcar `done` (claude-code-skills) — mapeamento completo em `../../docs/AGENT-SKILLS.md`.
  Instaladas em 2026-08-02 (escopo `user`), ver `../../docs/DECISIONS-LOG.md`.

## Definição de pronto (Definition of Done)

Uma feature deste serviço só está `done` quando (done only when):

> **Antes de começar** (não é item de `done`, é pré-requisito de `in-progress`): o campo
> `plan_review` daquela feature em `feature_list.json` precisa estar preenchido com o
> resultado do `Plan Reviewer` — ver `CLAUDE.md` da raiz, seção "Regras de trabalho".


- [ ] Implementada e rodando via `./init.sh` sem erro (`mvn verify`, gate de cobertura incluso).
- [ ] Regras de negócio relevantes (RN04, RN06, RN08, RN09) cobertas por teste (ver
      `../../docs/TESTING.md`).
- [ ] Idempotência do consumo testada (mensagem duplicada, via `PROCESSED_EVENT`, não duplica
      linhas nem métricas em `FACT_BET`).
- [ ] `Delivery Reviewer`, `Test Suite Auditor` e `Persistence Auditor` rodados contra a feature
      (ver `../../docs/AGENT-SKILLS.md`).
- [ ] `CHANGELOG.md` deste serviço tem uma entrada em `[Unreleased]` descrevendo a mudança.
- [ ] `feature_list.json` atualizado com status e evidência.
- [ ] `../../feature_list.json` (raiz) atualizado se este foi o marco que fecha `epic-004`.

## Fim de sessão (End of Session)

Antes de encerrar (before ending a session): atualize `progress.md` deste serviço, atualize
`feature_list.json`, e deixe `./init.sh` passando (clean, restartable state) — stay in scope:
não edite código de `bets-service` aqui, mesmo que a dúvida seja sobre o contrato do evento
(ajuste a nota do vault e sinalize, não o código do outro serviço).

## Verificação

```bash
./init.sh
```

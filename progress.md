# Log de Progresso — stats-service

## Estado Atual (Current State)

**Última atualização:** 2026-07-30 00:00
**Feature ativa:** nenhuma

## Status

### O que está pronto

- [x] Harness deste serviço criado.

### Em andamento

- Nenhuma feature iniciada.

### Próximos passos (Next Steps)

1. `feat-001` — inicializar o projeto Spring Boot 4.x com Maven (build tool já decidido em
   `../../docs/CONVENTIONS.md`) e o consumidor RabbitMQ.

## Bloqueios / Riscos

- Depende de `bets-service` já publicar `ApostaCriada` (epic-003) para testar o consumo
  ponta a ponta — pode ser desenvolvido com um publisher de teste antes disso.

## Decisões tomadas

- Build tool: **Maven**. Arquitetura: **hexagonal** (domain/application/adapter). Ambas
  decididas em `../../docs/CONVENTIONS.md`, não específicas desta sessão.

## Arquivos modificados nesta sessão

- `CLAUDE.md`, `feature_list.json`, `init.sh`, `progress.md`, `session-handoff.md` — criados.

## Evidência de conclusão

- Não aplicável ainda.

## Notas para a próxima sessão

Ver `../../docs/services/stats-service.md` para o esquema estrela completo e as chaves Redis
antes de começar `feat-002`.

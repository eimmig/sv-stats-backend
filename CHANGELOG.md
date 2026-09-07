# Changelog

Cada linha de `[Unreleased]` é um link para a issue do Jira que a gerou (story ou subtask),
formato `- [chave](url) - título` — sem prosa, sem categoria. Escrita automaticamente por
`tools/jira_story.py` no momento em que a issue é criada (ver `docs/CI-CD.md` seção "Changelog
por serviço"). O "porquê" de cada mudança vive na issue e na mensagem de commit, não aqui.

## [Unreleased]

- [SV-9](https://stakevault.atlassian.net/browse/SV-9) - Alinhar as chaves de projeto ao padrão do SonarCloud
- [SV-110](https://stakevault.atlassian.net/browse/SV-110) - Setup do projeto + consumidor RabbitMQ
- [SV-111](https://stakevault.atlassian.net/browse/SV-111) - Endurecer pipeline de CI antes do bootstrap (porta o padrao ja validado de bets-service)
- [SV-112](https://stakevault.atlassian.net/browse/SV-112) - Bootstrap do pom.xml e esqueleto hexagonal
- [SV-113](https://stakevault.atlassian.net/browse/SV-113) - Conexao Postgres com profiles dev/test/prod
- [SV-114](https://stakevault.atlassian.net/browse/SV-114) - Provisionamento de schema de tenant (Flyway lazy) + filtro X-Tenant-Id + rota admin X-Admin-Api-Key
- [SV-115](https://stakevault.atlassian.net/browse/SV-115) - Gate de cobertura JaCoCo 80%
- [SV-116](https://stakevault.atlassian.net/browse/SV-116) - Scaffold de i18n (MessageSource) e teste smoke
- [SV-117](https://stakevault.atlassian.net/browse/SV-117) - Health checks do Actuator
- [SV-118](https://stakevault.atlassian.net/browse/SV-118) - .env.example e logging estruturado
- [SV-119](https://stakevault.atlassian.net/browse/SV-119) - Consumidor RabbitMQ BetCreated/BetSettled (adapter/in/messaging)
- [SV-120](https://stakevault.atlassian.net/browse/SV-120) - CHANGELOG e verificacao final
- [SV-124](https://stakevault.atlassian.net/browse/SV-124) - Modelo OLAP (esquema estrela) + idempotencia
- [SV-125](https://stakevault.atlassian.net/browse/SV-125) - Migration Flyway do esquema estrela + PROCESSED_EVENT
- [SV-126](https://stakevault.atlassian.net/browse/SV-126) - Multi-tenancy do Hibernate + enforcement de X-Tenant-Id
- [SV-127](https://stakevault.atlassian.net/browse/SV-127) - Entidades JPA e persistencia (domain + adapter)
- [SV-128](https://stakevault.atlassian.net/browse/SV-128) - CHANGELOG e verificacao final
- [SV-129](https://stakevault.atlassian.net/browse/SV-129) - Consumo dos eventos - insert em BetCreated, upsert em BetSettled
- [SV-130](https://stakevault.atlassian.net/browse/SV-130) - Upsert real em FactBetRepository + resolucao de dimensoes
- [SV-131](https://stakevault.atlassian.net/browse/SV-131) - Plugar BetCreated/BetSettled no BetEventListener com idempotencia real
- [SV-132](https://stakevault.atlassian.net/browse/SV-132) - CHANGELOG e verificacao final
- [SV-133](https://stakevault.atlassian.net/browse/SV-133) - RF09 - Calculo de metricas
- [SV-134](https://stakevault.atlassian.net/browse/SV-134) - Agregacao real em FactBetRepository (overall + segmentado por sport/market/betting-house)
- [SV-135](https://stakevault.atlassian.net/browse/SV-135) - Servico de calculo de metricas (ROI, taxa de acerto) a partir do agregado
- [SV-136](https://stakevault.atlassian.net/browse/SV-136) - CHANGELOG e verificacao final
- [SV-137](https://stakevault.atlassian.net/browse/SV-137) - Cache Redis cache-aside
- [SV-138](https://stakevault.atlassian.net/browse/SV-138) - Bootstrap Redis + porta/adapter de cache (sem invalidacao ainda)
- [SV-139](https://stakevault.atlassian.net/browse/SV-139) - Agregacao mensal + orquestracao cache-aside + invalidacao no consumo do evento
- [SV-140](https://stakevault.atlassian.net/browse/SV-140) - CHANGELOG e verificacao final
- [SV-141](https://stakevault.atlassian.net/browse/SV-141) - RF11 - Endpoint GET /api/v1/statistics com filtros dinamicos
- [SV-142](https://stakevault.atlassian.net/browse/SV-142) - StatisticsFilter + agregacao filtrada em FactBetRepository/CalculateMetricsUseCase
- [SV-143](https://stakevault.atlassian.net/browse/SV-143) - Bundle de dashboard + endpoint GET /api/v1/statistics
- [SV-144](https://stakevault.atlassian.net/browse/SV-144) - CHANGELOG e verificacao final

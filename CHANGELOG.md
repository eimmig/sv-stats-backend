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

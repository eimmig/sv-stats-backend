-- epic-011 (raiz): DIM_TEAM nova + odd em FACT_BET, para GET /api/v1/statistics/search.
-- team1/team2 nao tem catalogo em bets-service (texto livre por aposta) - DIM_TEAM e resolvida
-- por nome (chave natural), nao por id vindo do evento, mesmo padrao de DIM_DATE. Uma aposta
-- referencia ate 2 times - FACT_BET ganha team1_id/team2_id (nullable) em vez de uma dimensao-
-- ponte. odd ja trafegava no evento desde o inicio mas nunca era persistida.
CREATE TABLE dim_team (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_dim_team_name UNIQUE (name)
);

ALTER TABLE fact_bet
    ADD COLUMN team1_id UUID REFERENCES dim_team (id),
    ADD COLUMN team2_id UUID REFERENCES dim_team (id),
    ADD COLUMN odd NUMERIC(19, 2);

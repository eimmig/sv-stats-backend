-- feat-013: DIM_TEAM ganha sportId (decisao do usuario) - o mesmo nome de time pode existir em
-- esportes diferentes, entao name sozinho nao e chave natural suficiente. dim_team foi
-- introduzida nesta mesma sessao (V20260910120000), nunca usada em tenant real - ALTER TABLE
-- direto, sem backfill (nao ha como inferir sportId de uma linha existente sem essa coluna).
ALTER TABLE dim_team DROP CONSTRAINT uq_dim_team_name;

ALTER TABLE dim_team
    ADD COLUMN sport_id UUID NOT NULL REFERENCES dim_sport (id),
    ADD CONSTRAINT uq_dim_team_name_sport UNIQUE (name, sport_id);

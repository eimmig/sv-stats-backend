-- epic-014 (raiz): FACT_BET.betType (nullable) - PRE/LIVE, a partir de BET.betType (bets-service
-- epic-013, ja enum la). Gravado so no insert de BetCreated, nunca sobrescrito pelo upsert de
-- BetSettled (o payload daquele evento nao carrega betType) - mesmo padrao de team1Id/team2Id/odd
-- (epic-011). Sem CHECK: este servico so reflete o valor recebido no evento, nao valida entrada
-- (bets-service ja garante pre/live via enum na origem).
ALTER TABLE fact_bet
    ADD COLUMN bet_type VARCHAR(4);

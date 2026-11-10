-- Estatistica sobre a data montada em dim_date (feat-039). O filtro de periodo das consultas em
-- fact_bet usa make_date(year, month, day) BETWEEN ...; sem estatistica sobre a expressao, o
-- PostgreSQL estima 2 dias para qualquer intervalo e escolhe uma busca no idx_fact_bet_date_id por
-- dia mesmo no ano inteiro. Com ela, a estimativa segue o numero real de dias.
CREATE STATISTICS dim_date_day ON (make_date(year, month, day)) FROM dim_date;

ANALYZE dim_date;

-- Somas diarias por segmento para o dashboard com periodo (feat-036). Cada coluna de BetAggregate
-- e soma ou contagem, entao somar os dias do intervalo reproduz as agregacoes de fact_bet. A media
-- da odd vira odd_sum / odd_count (AVG ignora odd nula) e a implicita, implied_sum /
-- decided_with_odd_count.
CREATE INDEX idx_fact_bet_date_id ON fact_bet (date_id);

CREATE TABLE agg_daily_segment (
    day DATE NOT NULL,
    segment VARCHAR(16) NOT NULL,
    dimension_key VARCHAR(36) NOT NULL,
    total_staked NUMERIC NOT NULL,
    net_profit NUMERIC NOT NULL,
    won_count BIGINT NOT NULL,
    lost_count BIGINT NOT NULL,
    void_count BIGINT NOT NULL,
    pre_count BIGINT NOT NULL,
    live_count BIGINT NOT NULL,
    settled_count BIGINT NOT NULL,
    odd_sum NUMERIC NOT NULL,
    odd_count BIGINT NOT NULL,
    implied_sum NUMERIC NOT NULL,
    decided_with_odd_count BIGINT NOT NULL,
    profit_squared_sum NUMERIC NOT NULL,
    stake_squared_sum NUMERIC NOT NULL,
    profit_stake_sum NUMERIC NOT NULL,
    PRIMARY KEY (day, segment, dimension_key)
);

-- Dias cujas somas estao desatualizadas. A leitura so usa agg_daily_segment quando nenhum dia do
-- intervalo esta aqui; o agendador recalcula e remove.
CREATE TABLE agg_dirty_day (
    day DATE PRIMARY KEY
);

-- Um evento pode mudar a data de um fato ja gravado, por isso o dia antigo e o novo sao marcados.
-- SET search_path FROM CURRENT fixa o schema da organizacao em que a migration rodou.
CREATE FUNCTION mark_agg_dirty_day() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path FROM CURRENT
AS $$
BEGIN
    IF TG_OP <> 'INSERT' THEN
        INSERT INTO agg_dirty_day (day)
        SELECT make_date(d.year, d.month, d.day) FROM dim_date d WHERE d.id = OLD.date_id
        ON CONFLICT DO NOTHING;
    END IF;
    IF TG_OP <> 'DELETE' THEN
        INSERT INTO agg_dirty_day (day)
        SELECT make_date(d.year, d.month, d.day) FROM dim_date d WHERE d.id = NEW.date_id
        ON CONFLICT DO NOTHING;
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_fact_bet_agg_dirty_day
    AFTER INSERT OR UPDATE OR DELETE ON fact_bet
    FOR EACH ROW EXECUTE FUNCTION mark_agg_dirty_day();

-- Carga inicial: todos os dias existentes ficam pendentes ate o primeiro recalculo.
INSERT INTO agg_dirty_day (day)
SELECT DISTINCT make_date(year, month, day) FROM dim_date;

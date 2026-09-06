CREATE TABLE dim_date (
    id UUID PRIMARY KEY,
    day INT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    quarter INT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL
);

CREATE TABLE dim_betting_house (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE dim_sport (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE dim_league (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE dim_market (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE dim_tipster (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE fact_bet (
    id UUID PRIMARY KEY,
    date_id UUID NOT NULL REFERENCES dim_date (id),
    betting_house_id UUID NOT NULL REFERENCES dim_betting_house (id),
    sport_id UUID NOT NULL REFERENCES dim_sport (id),
    league_id UUID NOT NULL REFERENCES dim_league (id),
    market_id UUID NOT NULL REFERENCES dim_market (id),
    tipster_id UUID REFERENCES dim_tipster (id),
    stake NUMERIC(19, 2) NOT NULL,
    profit NUMERIC(19, 2),
    is_win BOOLEAN,
    status VARCHAR(20) NOT NULL,
    bet_count INT NOT NULL DEFAULT 1
);

-- Idempotencia do consumo de evento - eventId unico impede reprocessar a mesma mensagem
-- (redelivery do RabbitMQ) mais de uma vez, verificado antes de processar e inserido na
-- mesma transacao (feat-003), nao so uma checagem em nivel de aplicacao.
CREATE TABLE processed_event (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_processed_event_event_id UNIQUE (event_id)
);

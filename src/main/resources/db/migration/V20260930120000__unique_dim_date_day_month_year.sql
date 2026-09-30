ALTER TABLE dim_date
    ADD CONSTRAINT uq_dim_date_day_month_year UNIQUE (day, month, year);

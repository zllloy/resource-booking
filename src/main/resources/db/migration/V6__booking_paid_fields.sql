ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS paid_at timestamptz,
    ADD COLUMN IF NOT EXISTS paid_by varchar(255);

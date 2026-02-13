CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Запрещаю пересечение интервалов по одному resource_id
ALTER TABLE booking
    ADD CONSTRAINT booking_no_overlap
    EXCLUDE USING gist (
  resource_id WITH =,
  tstzrange(start_time, end_time, '[)') WITH &&
)
WHERE (status IN ('WAITING_PAYMENT', 'CONFIRMED'));

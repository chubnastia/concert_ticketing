-- V3: constraints and uniques (idempotent)

-- Add CHECK (quantity BETWEEN 1 AND 6) if missing
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    WHERE c.conname = 'chk_reservation_quantity'
      AND t.relname = 'reservation'
      AND n.nspname = 'public'
  ) THEN
    ALTER TABLE public.reservation
      ADD CONSTRAINT chk_reservation_quantity CHECK (quantity BETWEEN 1 AND 6);
  END IF;
END$$;

-- One sale per reservation (idempotent buy) via unique index
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_indexes
    WHERE schemaname = 'public'
      AND tablename  = 'sale'
      AND indexname  = 'uniq_sale_reservation'
  ) THEN
    CREATE UNIQUE INDEX uniq_sale_reservation ON public.sale(reservation_id);
  END IF;
END$$;

-- Helpful index for expiry sweep (safe if already created earlier)
CREATE INDEX IF NOT EXISTS idx_reservation_status_expiry ON public.reservation(status, expiry);

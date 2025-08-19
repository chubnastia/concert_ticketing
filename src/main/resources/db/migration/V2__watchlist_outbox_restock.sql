-- V2: watchlist + outbox + restock transition tracking

-- Track sold-out state and a monotonic token for 0->>0 transitions
ALTER TABLE concert
  ADD COLUMN IF NOT EXISTS sold_out boolean NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS restock_token integer NOT NULL DEFAULT 0;

-- Initialize from current availability
UPDATE concert SET sold_out = (available_tickets = 0);

-- Watchlist (one email per concert)
CREATE TABLE IF NOT EXISTS watchlist (
  id BIGSERIAL PRIMARY KEY,
  concert_id BIGINT NOT NULL REFERENCES concert(id) ON DELETE CASCADE,
  email TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (concert_id, email)
);

-- Outbox for one-shot notifications per 0->>0 transition
CREATE TABLE IF NOT EXISTS outbox (
  id BIGSERIAL PRIMARY KEY,
  type TEXT NOT NULL,                 -- e.g., WATCHLIST_RESTOCK
  aggregate_id BIGINT NOT NULL,       -- concert id
  token INT NOT NULL,                 -- restock_token snapshot
  payload TEXT NOT NULL,              -- keep as TEXT for JPA String
  status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING | SENT
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (type, aggregate_id, token)
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_reservation_status_expiry ON reservation(status, expiry);
CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox(status);

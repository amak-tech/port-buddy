-- Add creator user reference to port_reservations
ALTER TABLE port_reservations
    ADD COLUMN IF NOT EXISTS user_id UUID NULL REFERENCES users (id);

-- Index for faster lookups by user
CREATE INDEX IF NOT EXISTS idx_port_reservations_user_id ON port_reservations (user_id);

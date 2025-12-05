/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

-- Link tunnels to port_reservations
ALTER TABLE IF EXISTS tunnels
    ADD COLUMN IF NOT EXISTS port_reservation_id UUID NULL REFERENCES port_reservations (id);

CREATE INDEX IF NOT EXISTS idx_tunnels_port_reservation_id ON tunnels (port_reservation_id);

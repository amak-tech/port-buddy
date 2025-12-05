/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

-- Add soft delete flag and change unique constraint to only apply to non-deleted records
ALTER TABLE port_reservations
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Drop old unique constraint and replace with a partial unique index
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        WHERE t.relname = 'port_reservations' AND c.conname = 'uq_port_reservations_host_port'
    ) THEN
        ALTER TABLE port_reservations DROP CONSTRAINT uq_port_reservations_host_port;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_port_reservations_host_port_active
    ON port_reservations(public_host, public_port)
    WHERE deleted = FALSE;

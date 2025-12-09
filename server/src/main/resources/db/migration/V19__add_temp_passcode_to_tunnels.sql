/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

-- Temporary passcode hash per tunnel (set via CLI during expose)
ALTER TABLE tunnels
    ADD COLUMN IF NOT EXISTS temp_passcode_hash TEXT;

/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

-- Add optional passcode hash column to domains for securing HTTP tunnels by domain
ALTER TABLE domains
    ADD COLUMN IF NOT EXISTS passcode_hash TEXT;

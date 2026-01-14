/*
 * Copyright (c) 2026 AMAK Inc. All rights reserved.
 */

ALTER TABLE domains ADD COLUMN ssl_active BOOLEAN NOT NULL DEFAULT FALSE;

/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

ALTER TABLE accounts ADD COLUMN extra_tunnels INTEGER NOT NULL DEFAULT 0;

UPDATE accounts SET plan = 'PRO' WHERE plan IN ('BASIC', 'INDIVIDUAL');
UPDATE accounts SET plan = 'TEAM' WHERE plan = 'PROFESSIONAL';
UPDATE accounts SET plan = 'PRO' WHERE plan NOT IN ('PRO', 'TEAM');

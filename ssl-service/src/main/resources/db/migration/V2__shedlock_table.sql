/*
 * Copyright (c) 2026 AMAK Inc. All rights reserved.
 */

-- ShedLock table for cluster-safe scheduling
CREATE TABLE IF NOT EXISTS shedlock
(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

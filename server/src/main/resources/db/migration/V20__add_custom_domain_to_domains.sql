/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

ALTER TABLE domains ADD COLUMN custom_domain VARCHAR(255);
ALTER TABLE domains ADD COLUMN cname_verified BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_domains_custom_domain ON domains(custom_domain);

/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

CREATE TABLE ip_blacklist (
    id UUID PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL,
    account_id UUID NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_ip_blacklist_ip_account UNIQUE (ip_address, account_id)
);

CREATE INDEX idx_ip_blacklist_ip ON ip_blacklist(ip_address);
CREATE INDEX idx_ip_blacklist_account ON ip_blacklist(account_id);

-- Backfill step A: seed the blacklist with the distinct client IPs of tunnels
-- belonging to accounts that are already blocked.
INSERT INTO ip_blacklist (id, ip_address, account_id, reason)
SELECT gen_random_uuid(), t.client_ip, t.account_id, 'account blocked'
FROM (
    SELECT DISTINCT client_ip, account_id
    FROM tunnels
    WHERE client_ip IS NOT NULL
      AND account_id IN (SELECT id FROM accounts WHERE blocked = true)
) t
ON CONFLICT (ip_address, account_id) DO NOTHING;

-- Backfill step B: block any still-unblocked account that has a tunnel originating
-- from an IP that is now blacklisted (flag only; active tunnels are left untouched).
UPDATE accounts
SET blocked = true,
    updated_at = now()
WHERE blocked = false
  AND id IN (
      SELECT DISTINCT t.account_id
      FROM tunnels t
      WHERE t.client_ip IS NOT NULL
        AND t.client_ip IN (SELECT ip_address FROM ip_blacklist)
  );

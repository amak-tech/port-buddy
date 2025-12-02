ALTER TABLE tunnels ADD COLUMN domain_id UUID;

ALTER TABLE tunnels ADD CONSTRAINT fk_tunnels_domain_id FOREIGN KEY (domain_id) REFERENCES domains(id);

CREATE INDEX idx_tunnels_domain_id ON tunnels(domain_id);

-- Backfill domain_id for existing HTTP tunnels
UPDATE tunnels t
SET domain_id = d.id
FROM domains d
WHERE t.subdomain = d.subdomain AND t.account_id = d.account_id
  AND t.type = 'HTTP';

-- Insert missing domains for orphans (if any)
INSERT INTO domains (id, subdomain, domain, account_id, created_at, updated_at)
SELECT gen_random_uuid(), t.subdomain, 'portbuddy.dev', t.account_id, NOW(), NOW()
FROM tunnels t
WHERE t.type = 'HTTP'
  AND t.domain_id IS NULL
  AND t.subdomain IS NOT NULL
GROUP BY t.subdomain, t.account_id;

-- Run update again to link newly created domains
UPDATE tunnels t
SET domain_id = d.id
FROM domains d
WHERE t.subdomain = d.subdomain AND t.account_id = d.account_id
  AND t.type = 'HTTP'
  AND t.domain_id IS NULL;

ALTER TABLE tunnels DROP COLUMN subdomain;

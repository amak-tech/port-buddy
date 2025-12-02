ALTER TABLE tunnels ADD COLUMN account_id UUID;

UPDATE tunnels t
SET account_id = u.account_id
FROM users u
WHERE t.user_id = u.id;

ALTER TABLE tunnels ALTER COLUMN account_id SET NOT NULL;

ALTER TABLE tunnels ADD CONSTRAINT fk_tunnels_account_id FOREIGN KEY (account_id) REFERENCES accounts(id);

CREATE INDEX idx_tunnels_account_id ON tunnels(account_id);

ALTER TABLE tunnels DROP COLUMN user_id;

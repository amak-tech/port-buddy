ALTER TABLE api_keys ADD COLUMN account_id UUID;

UPDATE api_keys k
SET account_id = u.account_id
FROM users u
WHERE k.user_id = u.id;

ALTER TABLE api_keys ALTER COLUMN account_id SET NOT NULL;

ALTER TABLE api_keys ADD CONSTRAINT fk_api_keys_account_id FOREIGN KEY (account_id) REFERENCES accounts(id);

CREATE INDEX idx_api_keys_account_id ON api_keys(account_id);

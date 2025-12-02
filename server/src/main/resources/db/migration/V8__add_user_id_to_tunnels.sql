ALTER TABLE tunnels ADD COLUMN user_id UUID;

UPDATE tunnels t
SET user_id = ak.user_id
FROM api_keys ak
WHERE t.api_key_id = ak.id;

ALTER TABLE tunnels ADD CONSTRAINT fk_tunnels_user_id FOREIGN KEY (user_id) REFERENCES users(id);

CREATE INDEX idx_tunnels_user_id ON tunnels(user_id);

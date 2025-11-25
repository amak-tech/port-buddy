-- Tunnels storage
CREATE TABLE IF NOT EXISTS tunnels (
    id UUID PRIMARY KEY,
    tunnel_id VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    api_key_id UUID NULL REFERENCES api_keys(id) ON DELETE SET NULL,

    local_scheme VARCHAR(16) NULL,
    local_host VARCHAR(255) NULL,
    local_port INTEGER NULL,

    public_url VARCHAR(1024) NULL,
    public_host VARCHAR(255) NULL,
    public_port INTEGER NULL,
    subdomain VARCHAR(255) NULL,

    last_heartbeat_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tunnels_user_id ON tunnels(user_id);
CREATE INDEX IF NOT EXISTS idx_tunnels_api_key_id ON tunnels(api_key_id);
CREATE INDEX IF NOT EXISTS idx_tunnels_status ON tunnels(status);
CREATE INDEX IF NOT EXISTS idx_tunnels_created_at ON tunnels(created_at);

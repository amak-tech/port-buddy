-- Accounts and Users schema
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    email VARCHAR(320),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    auth_provider VARCHAR(100) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(1024),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_provider_ext UNIQUE (auth_provider, external_id)
);

CREATE INDEX IF NOT EXISTS idx_users_account_id ON users(account_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);

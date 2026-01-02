/*
 * Copyright (c) 2026 AMAK Inc. All rights reserved.
 */

-- V26__many_to_many_users_accounts.sql
CREATE TABLE IF NOT EXISTS user_accounts (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    roles VARCHAR(255)[] NOT NULL DEFAULT '{}',
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, account_id)
);

-- Migrate data
INSERT INTO user_accounts (user_id, account_id, roles, created_at, updated_at)
SELECT id, account_id, roles, created_at, updated_at FROM users;

-- Drop columns from users
ALTER TABLE users DROP COLUMN account_id;
ALTER TABLE users DROP COLUMN roles;

-- Update indexes
CREATE INDEX IF NOT EXISTS idx_user_accounts_user_id ON user_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_user_accounts_account_id ON user_accounts(account_id);

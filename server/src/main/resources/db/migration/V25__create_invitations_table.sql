/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

CREATE TABLE invitations (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id),
    email VARCHAR(320) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    invited_by_id UUID NOT NULL REFERENCES users(id),
    accepted_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invitations_account_id ON invitations(account_id);
CREATE INDEX idx_invitations_token ON invitations(token);
CREATE INDEX idx_invitations_email ON invitations(email);

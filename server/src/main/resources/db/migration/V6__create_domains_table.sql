CREATE TABLE domains (
    id UUID PRIMARY KEY,
    subdomain VARCHAR(255) NOT NULL UNIQUE,
    domain VARCHAR(255) NOT NULL,
    account_id UUID NOT NULL REFERENCES accounts(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_domains_account_id ON domains(account_id);

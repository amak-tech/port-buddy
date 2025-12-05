CREATE TABLE port_reservations (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id),
    public_host VARCHAR(255) NOT NULL,
    public_port INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_port_reservations_host_port UNIQUE (public_host, public_port)
);

CREATE INDEX idx_port_reservations_account_id ON port_reservations(account_id);
CREATE INDEX idx_port_reservations_public_host ON port_reservations(public_host);

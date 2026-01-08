/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

-- Create tables for SSL certificates and jobs

CREATE TABLE IF NOT EXISTS ssl_certificates (
    id UUID PRIMARY KEY,
    domain VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    managed BOOLEAN NOT NULL DEFAULT FALSE,
    verification_method VARCHAR(64) NULL,
    contact_email VARCHAR(255) NULL,
    issued_at TIMESTAMPTZ NULL,
    expires_at TIMESTAMPTZ NULL,
    certificate_path VARCHAR(1024) NULL,
    private_key_path VARCHAR(1024) NULL,
    chain_path VARCHAR(1024) NULL,
    created_by VARCHAR(100) NULL,
    created_at TIMESTAMPTZ NULL,
    updated_by VARCHAR(100) NULL,
    updated_at TIMESTAMPTZ NULL
);

CREATE TABLE IF NOT EXISTS ssl_certificate_jobs (
    id UUID PRIMARY KEY,
    domain VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    message VARCHAR(2000) NULL,
    contact_email VARCHAR(255) NULL,
    challenge_records_json TEXT NULL,
    order_location VARCHAR(1024) NULL,
    authorization_urls_json TEXT NULL,
    challenge_expires_at TIMESTAMPTZ NULL,
    managed BOOLEAN NOT NULL DEFAULT FALSE,
    started_at TIMESTAMPTZ NULL,
    finished_at TIMESTAMPTZ NULL,
    created_by VARCHAR(100) NULL,
    created_at TIMESTAMPTZ NULL,
    updated_by VARCHAR(100) NULL,
    updated_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_ssl_certificate_jobs_domain ON ssl_certificate_jobs(domain);

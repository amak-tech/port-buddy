/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

CREATE TABLE email_otps (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    code_hash VARCHAR(255) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_otps_email ON email_otps(email);

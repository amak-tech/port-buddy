/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

ALTER TABLE accounts ADD COLUMN stripe_customer_id VARCHAR(255);
ALTER TABLE accounts ADD COLUMN stripe_subscription_id VARCHAR(255);
ALTER TABLE accounts ADD COLUMN subscription_status VARCHAR(50);

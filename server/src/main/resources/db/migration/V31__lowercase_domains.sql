
UPDATE domains
SET subdomain = LOWER(subdomain),
    domain = LOWER(domain),
    custom_domain = LOWER(custom_domain);

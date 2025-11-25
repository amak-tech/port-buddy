-- ShedLock table for cluster-safe scheduling
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- Helpful index to speed up stale selection/updates
CREATE INDEX IF NOT EXISTS idx_tunnels_status_last_heartbeat ON tunnels (status, last_heartbeat_at);

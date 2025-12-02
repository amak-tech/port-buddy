-- Drop obsolete tunnel_id column; entity id (UUID) is used as the tunnel identifier now
ALTER TABLE IF EXISTS tunnels
    DROP COLUMN IF EXISTS tunnel_id;

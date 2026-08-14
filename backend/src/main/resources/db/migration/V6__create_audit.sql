CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    actor_user_id UUID NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100) NULL,
    correlation_id VARCHAR(100) NULL,
    ip_address VARCHAR(64) NULL,
    metadata JSONB NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_audit_log_actor_user FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

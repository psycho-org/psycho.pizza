CREATE TABLE IF NOT EXISTS analysis_request (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    requested_by UUID,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP(6),
    completed_at TIMESTAMP(6),
    error_message TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS analysis_report (
    id UUID PRIMARY KEY,
    analysis_request_id UUID NOT NULL,
    run_id VARCHAR(100),
    workspace_id UUID NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    score_total INT NOT NULL,
    score_version VARCHAR(50) NOT NULL,
    category_penalties JSONB NOT NULL,
    penalty_details JSONB NOT NULL,
    generated_at TIMESTAMP(6) NOT NULL,
    ai_insight TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6),
    deleted_by UUID
);

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    actor_id UUID,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    from_value TEXT,
    to_value TEXT,
    occurred_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

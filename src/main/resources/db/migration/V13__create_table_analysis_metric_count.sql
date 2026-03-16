CREATE TABLE IF NOT EXISTS analysis_metric_count (
       id UUID PRIMARY KEY,
       created_at TIMESTAMP(6) NOT NULL,
       updated_at TIMESTAMP(6) NOT NULL,

       workspace_id UUID NOT NULL,
       sprint_id UUID NOT NULL,
       event_subtype VARCHAR(100) NOT NULL,
       count INTEGER NOT NULL DEFAULT 0,

       CONSTRAINT uq_analysis_metric_count
           UNIQUE (workspace_id, sprint_id, event_subtype)
);

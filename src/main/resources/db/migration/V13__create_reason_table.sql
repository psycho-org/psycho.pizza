create table if not exists public.reasons
(
    id uuid primary key not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    target_id uuid not null,
    target_type character varying(20) not null,
    reason text not null,
    event_id uuid not null,
    event_type character varying(20) not null,
    workspace_id uuid not null
);

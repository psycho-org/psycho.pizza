-- projects 테이블 생성
create table if not exists public.sprints
(
    id uuid primary key not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    deleted_at timestamp(6),
    deleted_by uuid,
    name character varying(512) not null,
    start_date timestamp(6) not null,
    end_date timestamp(6) not null,
    workspace_id uuid not null
);

-- project, task 매핑 테이블 생성
create table if not exists public.sprint_project_mapping
(
    id uuid primary key not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    sprint_id uuid not null,
    project_id uuid not null,
    workspace_id uuid not null
);

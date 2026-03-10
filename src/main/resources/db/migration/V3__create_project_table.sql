-- projects 테이블 생성
create table if not exists public.projects
(
    id           uuid primary key       not null,
    created_at   timestamp(6)           not null,
    updated_at   timestamp(6)           not null,
    deleted_at   timestamp(6),
    deleted_by   uuid,
    name         character varying(512) not null,
    workspace_id uuid                   not null
);

-- project, task 매핑 테이블 생성
create table if not exists public.project_task_mapping
(
    id           uuid primary key not null,
    created_at   timestamp(6)     not null,
    updated_at   timestamp(6)     not null,
    project_id   uuid             not null,
    task_id      uuid             not null,
    workspace_id uuid             not null
);

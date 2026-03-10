create table if not exists public.mail_templates (
    id uuid primary key not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    deleted_at timestamp(6),
    deleted_by uuid,
    mail_type character varying(50) not null,
    title text not null,
    description character varying(255) not null,
    action_type character varying(50),
    token_auth_enabled boolean not null default false,
    token_expire_hours bigint,
    html_content text not null
);

create table if not exists public.message_auth_tokens (
    id uuid primary key not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    mail_type character varying(50) not null,
    channel character varying(20) not null,
    action_type character varying(50) not null,
    target_email character varying(255) not null,
    context_key character varying(100),
    token character varying(64) not null,
    expired_at timestamp(6) not null,
    verified_at timestamp(6),
    failure_reason character varying(30),
    failed_at timestamp(6),
    action_status character varying(20),
    action_error text,
    action_processed_at timestamp(6)
);

create unique index if not exists
uk_message_auth_tokens_token
on message_auth_tokens using btree (
    token
);

create index if not exists
idx_message_auth_tokens_target
on message_auth_tokens using btree (
    mail_type,
    lower(target_email),
    coalesce(context_key, '')
);

create unique index if not exists
ux_message_auth_tokens_pending
on message_auth_tokens (
    mail_type,
    lower(target_email),
    coalesce(context_key, '')
)
where (verified_at is null and failure_reason is null);

create table if not exists public.message_auth_token_params (
    id uuid primary key not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    message_auth_token_id uuid not null,
    name character varying(100) not null,
    value character varying(2048) not null,
    foreign key (message_auth_token_id) references public.message_auth_tokens (
        id
    ) on delete cascade
);

create unique index if not exists
uk_message_auth_token_params_token_name
on message_auth_token_params (
    message_auth_token_id, name
);

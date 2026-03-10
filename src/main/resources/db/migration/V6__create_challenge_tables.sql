-- challenges 테이블 생성
create table if not exists public.challenges
(
    id uuid primary key not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    operation_type character varying(30) not null,
    target_email character varying(255) not null,
    otp_hash character varying(255) not null,
    expires_at timestamp(6) not null,
    attempt_count integer not null default 0,
    max_attempts integer not null default 5,
    status character varying(20) not null default 'PENDING'
);

-- PENDING 상태의 이메일+작업 유형 유니크 제약
create unique index if not exists
uk_challenges_email_op_pending
on challenges using btree (lower(target_email), operation_type)
where (status = 'PENDING');

-- confirmation_tokens 테이블 생성
create table if not exists public.confirmation_tokens
(
    id uuid primary key not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    challenge_id uuid not null,
    operation_type character varying(30) not null,
    target_email character varying(255) not null,
    expires_at timestamp(6) not null,
    used boolean not null default false,
    foreign key (challenge_id) references public.challenges (id)
    match simple on update no action on delete no action
);

-- accounts 테이블 생성
create table if not exists public.accounts (
                                 id uuid primary key not null,
                                 created_at timestamp(6) with time zone not null,
                                 updated_at timestamp(6) with time zone,
                                 deleted_at timestamp(6) with time zone,
                                 deleted_by uuid,
                                 display_name character varying(255) not null,
                                 email character varying(255) not null,
                                 family_name character varying(255) not null,
                                 given_name character varying(255) not null,
                                 password_hash character varying(255) not null
);
-- accounts 테이블 email 유니크키 추가
create unique index if not exists uk_accounts_email on accounts using btree (email) WHERE (deleted_at IS NULL);

-- refresh_tokens 테이블 생성
create table if not exists public.refresh_tokens (
                                       id uuid primary key not null,
                                       created_at timestamp(6) with time zone not null,
                                       updated_at timestamp(6) with time zone,
                                       account_id uuid not null,
                                       expires_at timestamp(6) with time zone not null,
                                       jti character varying(64) not null,
                                       revoked_at timestamp(6) with time zone,
                                       token_hash character varying(128) not null
);
-- refresh_tokens account_id 인덱스
create index if not exists idx_refresh_tokens_account_id on refresh_tokens using btree (account_id);

-- refresh_tokens jti 유니크 인덱스
create unique index if not exists uk18kw8ppjw4gmmgdns9ecyg17b on refresh_tokens using btree (jti);

-- refresh_tokens token_hash 유니크 인덱스
create unique index if not exists uko2mlirhldriil2y7krapq4frt on refresh_tokens using btree (token_hash);


-- workspaces 테이블 생성
create table if not exists public.workspaces (
                                   id uuid primary key not null,
                                   created_at timestamp(6) with time zone not null,
                                   updated_at timestamp(6) with time zone,
                                   deleted_at timestamp(6) with time zone,
                                   deleted_by uuid,
                                   description character varying(255),
                                   name character varying(255) not null
);

-- memberships 테이블 생성
create table if not exists public.memberships (
                                    id uuid primary key not null,
                                    created_at timestamp(6) with time zone not null,
                                    updated_at timestamp(6) with time zone,
                                    deleted_at timestamp(6) with time zone,
                                    deleted_by uuid,
                                    account_id uuid not null,
                                    role character varying(50) not null,
                                    workspace_id uuid not null,
                                    foreign key (workspace_id) references public.workspaces (id)
                                        match simple on update no action on delete no action
);

-- memberships 활성 멤버십 그룹 유니크 인덱스 추가
create unique index if not exists uk_memberships_account_workspace_active on memberships using btree (account_id, workspace_id) WHERE (deleted_at IS NULL and deleted_by IS NULL);

-- tasks 테이블 생성
create table if not exists public.tasks (
                              id uuid primary key not null,
                              created_at timestamp(6) with time zone not null,
                              updated_at timestamp(6) with time zone,
                              deleted_at timestamp(6) with time zone,
                              deleted_by uuid,
                              assignee_id uuid,
                              description character varying(255) not null,
                              due_date timestamp(6) with time zone,
                              status character varying(20) not null,
                              title character varying(512) not null,
                              workspace_id uuid not null
);

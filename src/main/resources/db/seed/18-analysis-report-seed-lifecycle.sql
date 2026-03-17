-- Membership 이탈 / Workspace 종료 lifecycle 시드
-- 목적:
-- 1) W11_Member_Churn 에서 실제 membership 이탈 1건 반영
-- 2) W12_Workspace_Closed_Midflight 에서 진행 중 workspace 종료 반영
-- 주의:
-- - accounts / workspaces / memberships / sprints / projects / tasks 시드 적용 이후에 실행하는 것을 권장한다.

-- W11: 핵심 담당자(A024) 이탈
update public.memberships
set
    updated_at = now(),
    deleted_at = '2026-02-03 18:00:00'::timestamp,
    deleted_by = '00000000-0000-0000-0000-000000000001'::uuid
where workspace_id = '10000000-0000-0000-0000-000000000011'::uuid
  and account_id = '00000000-0000-0000-0000-000000000024'::uuid
  and deleted_at is null
  and deleted_by is null;

-- W12: workspace 진행 중 종료
update public.workspaces
set
    updated_at = now(),
    deleted_at = '2026-02-18 18:30:00'::timestamp,
    deleted_by = '00000000-0000-0000-0000-000000000001'::uuid
where id = '10000000-0000-0000-0000-000000000012'::uuid
  and deleted_at is null
  and deleted_by is null;

-- W12: workspace 종료 시점에 활성 membership도 함께 종료 처리
update public.memberships
set
    updated_at = now(),
    deleted_at = '2026-02-18 18:30:00'::timestamp,
    deleted_by = '00000000-0000-0000-0000-000000000001'::uuid
where workspace_id = '10000000-0000-0000-0000-000000000012'::uuid
  and deleted_at is null
  and deleted_by is null;

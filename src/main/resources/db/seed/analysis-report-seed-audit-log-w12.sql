-- W12_Workspace_Closed_Midflight 전용 Audit Log 시드
-- 로그 집계 요약:
-- - 직접 감점 후보 로그 수: 4건
--   - Due date 이후 DONE 처리 1건
--   - TODO -> DONE 직행 1건
--   - IN_PROGRESS -> TODO 상태 회귀 1건
--   - CANCELED 처리 1건
-- - 판단 유보 조정 로그 수: 5건
--   - SPRINT_GOAL_CHANGED 1건
--   - TASK_ADDED_TO_SPRINT 2건
--   - TASK_ASSIGNEE_CHANGED 1건
--   - TASK_DUE_DATE_CHANGED 1건
-- - 단순 진행 로그 수: 6건
--   - TASK_STATUS_CHANGED 6건
--
-- 의도:
-- 1) W12는 진행 중이던 workspace가 중간에 닫히는 종료 시나리오다.
-- 2) workspace 종료 자체는 analysis-report-seed-lifecycle.sql 에서 workspaces / memberships soft delete 로 반영한다.
-- 3) audit_log 에서는 종료 전까지 나타난 목표 축소, 급한 태스크 투입, 취소, 미완료 잔존을 중심으로 표현한다.
-- 4) 2026-02-18 18:30 이후에는 workspace 종료로 추가 진행 로그가 더 이상 발생하지 않는 흐름을 의도한다.

with task_lookup as (
    select
        t.id,
        t.title
    from public.tasks t
    where t.workspace_id = '10000000-0000-0000-0000-000000000012'::uuid
),
sprint_lookup as (
    select
        s.id,
        s.name
    from public.sprints s
    where s.workspace_id = '10000000-0000-0000-0000-000000000012'::uuid
),
audit_seed as (
    values
        -- Sprint 01
        (
            '41000000-0000-0000-0000-000000000001',
            '00000000-0000-0000-0000-000000000002',
            'TASK',
            'W12-S01 P01 - 존속 리스크 점검 - 현황 및 자료 수집 - 존속 리스크 점검 관련 자료와 선행 이슈 정리',
            'TASK_STATUS_CHANGED',
            'TODO',
            'IN_PROGRESS',
            '2026-01-05 09:00:00'
        ),
        (
            '41000000-0000-0000-0000-000000000002',
            '00000000-0000-0000-0000-000000000002',
            'TASK',
            'W12-S01 P01 - 존속 리스크 점검 - 현황 및 자료 수집 - 존속 리스크 점검 관련 자료와 선행 이슈 정리',
            'TASK_STATUS_CHANGED',
            'IN_PROGRESS',
            'DONE',
            '2026-01-08 18:10:00'
        ),

        -- Sprint 02
        (
            '41000000-0000-0000-0000-000000000003',
            '00000000-0000-0000-0000-000000000001',
            'SPRINT',
            'W12 Sprint 02 - 축소 운영 정리',
            'SPRINT_GOAL_CHANGED',
            '축소 운영 정리',
            '종료 대비 우선 정리',
            '2026-02-01 08:40:00'
        ),
        (
            '41000000-0000-0000-0000-000000000004',
            '00000000-0000-0000-0000-000000000001',
            'TASK',
            'W12-S02 P04 - 정산 마감 목록 정리 - 실행 준비 - 정산 마감 목록 정리 실행 전 준비 사항 점검',
            'TASK_ADDED_TO_SPRINT',
            null,
            '20000000-0000-0000-0000-000000000046',
            '2026-02-01 09:00:00'
        ),
        (
            '41000000-0000-0000-0000-000000000005',
            '00000000-0000-0000-0000-000000000028',
            'TASK',
            'W12-S02 P04 - 정산 마감 목록 정리 - 실행 준비 - 정산 마감 목록 정리 실행 전 준비 사항 점검',
            'TASK_STATUS_CHANGED',
            'TODO',
            'IN_PROGRESS',
            '2026-02-01 09:20:00'
        ),
        (
            '41000000-0000-0000-0000-000000000006',
            '00000000-0000-0000-0000-000000000001',
            'TASK',
            'W12-S02 P04 - 정산 마감 목록 정리 - 실행 준비 - 정산 마감 목록 정리 실행 전 준비 사항 점검',
            'TASK_DUE_DATE_CHANGED',
            '2026-02-08 23:59:59',
            '2026-02-10 23:59:59',
            '2026-02-07 14:00:00'
        ),
        -- 직접 감점 후보: 마감 목록 정리는 due date를 넘겨 끝남
        (
            '41000000-0000-0000-0000-000000000007',
            '00000000-0000-0000-0000-000000000028',
            'TASK',
            'W12-S02 P04 - 정산 마감 목록 정리 - 실행 준비 - 정산 마감 목록 정리 실행 전 준비 사항 점검',
            'TASK_STATUS_CHANGED',
            'IN_PROGRESS',
            'DONE',
            '2026-02-11 10:10:00'
        ),
        (
            '41000000-0000-0000-0000-000000000008',
            '00000000-0000-0000-0000-000000000011',
            'TASK',
            'W12-S02 P01 - 축소 운영 범위 확정 - 현황 및 자료 수집 - 축소 운영 범위 확정 관련 자료와 선행 이슈 정리',
            'TASK_STATUS_CHANGED',
            'TODO',
            'IN_PROGRESS',
            '2026-02-03 10:00:00'
        ),
        -- 직접 감점 후보: 종료 압박으로 다시 TODO로 회귀
        (
            '41000000-0000-0000-0000-000000000009',
            '00000000-0000-0000-0000-000000000011',
            'TASK',
            'W12-S02 P01 - 축소 운영 범위 확정 - 현황 및 자료 수집 - 축소 운영 범위 확정 관련 자료와 선행 이슈 정리',
            'TASK_STATUS_CHANGED',
            'IN_PROGRESS',
            'TODO',
            '2026-02-05 16:00:00'
        ),

        -- Sprint 03
        (
            '41000000-0000-0000-0000-000000000010',
            '00000000-0000-0000-0000-000000000001',
            'TASK',
            'W12-S03 P01 - 종료 공지 발송 - 현황 및 자료 수집 - 종료 공지 발송 관련 자료와 선행 이슈 정리',
            'TASK_ADDED_TO_SPRINT',
            null,
            '20000000-0000-0000-0000-000000000047',
            '2026-02-09 09:00:00'
        ),
        -- 직접 감점 후보: 종료 공지는 검토 없이 바로 발송
        (
            '41000000-0000-0000-0000-000000000011',
            '00000000-0000-0000-0000-000000000001',
            'TASK',
            'W12-S03 P01 - 종료 공지 발송 - 현황 및 자료 수집 - 종료 공지 발송 관련 자료와 선행 이슈 정리',
            'TASK_STATUS_CHANGED',
            'TODO',
            'DONE',
            '2026-02-10 11:20:00'
        ),
        (
            '41000000-0000-0000-0000-000000000012',
            '00000000-0000-0000-0000-000000000020',
            'TASK',
            'W12-S03 P02 - 잔여 정산 처리 - 초안 정리 - 잔여 정산 처리 초안과 진행 방향 구체화',
            'TASK_STATUS_CHANGED',
            'TODO',
            'IN_PROGRESS',
            '2026-02-12 10:00:00'
        ),
        (
            '41000000-0000-0000-0000-000000000013',
            '00000000-0000-0000-0000-000000000001',
            'TASK',
            'W12-S03 P03 - 종료 후 문의 대응 정리 - 현황 및 자료 수집 - 종료 후 문의 대응 정리 관련 자료와 선행 이슈 정리',
            'TASK_ASSIGNEE_CHANGED',
            '00000000-0000-0000-0000-000000000028',
            null,
            '2026-02-17 10:00:00'
        ),
        (
            '41000000-0000-0000-0000-000000000014',
            '00000000-0000-0000-0000-000000000001',
            'TASK',
            'W12-S03 P03 - 종료 후 문의 대응 정리 - 현황 및 자료 수집 - 종료 후 문의 대응 정리 관련 자료와 선행 이슈 정리',
            'TASK_STATUS_CHANGED',
            'TODO',
            'IN_PROGRESS',
            '2026-02-17 10:20:00'
        ),
        -- 직접 감점 후보: workspace 종료 직전 잔여 정산 처리도 취소
        (
            '41000000-0000-0000-0000-000000000015',
            '00000000-0000-0000-0000-000000000020',
            'TASK',
            'W12-S03 P02 - 잔여 정산 처리 - 초안 정리 - 잔여 정산 처리 초안과 진행 방향 구체화',
            'TASK_STATUS_CHANGED',
            'IN_PROGRESS',
            'CANCELLED',
            '2026-02-18 18:20:00'
        )
),
resolved_audit as (
    select
        s.column1::uuid as id,
        '10000000-0000-0000-0000-000000000012'::uuid as workspace_id,
        s.column2::uuid as actor_id,
        s.column3 as target_type,
        case
            when s.column3 = 'TASK' then t.id
            when s.column3 = 'SPRINT' then sp.id
            else null
        end as target_id,
        s.column5 as event_type,
        s.column6 as from_value,
        s.column7 as to_value,
        s.column8::timestamp as occurred_at
    from audit_seed s
    left join task_lookup t
        on s.column3 = 'TASK'
       and t.title = s.column4
    left join sprint_lookup sp
        on s.column3 = 'SPRINT'
       and sp.name = s.column4
)
insert into public.audit_log (
    id,
    workspace_id,
    actor_id,
    target_type,
    target_id,
    event_type,
    from_value,
    to_value,
    occurred_at,
    created_at,
    updated_at
)
select
    ra.id,
    ra.workspace_id,
    ra.actor_id,
    ra.target_type,
    ra.target_id,
    ra.event_type,
    ra.from_value,
    ra.to_value,
    ra.occurred_at,
    now(),
    now()
from resolved_audit ra
where ra.target_id is not null
  and not exists (
      select 1
      from public.audit_log al
      where al.id = ra.id
  );

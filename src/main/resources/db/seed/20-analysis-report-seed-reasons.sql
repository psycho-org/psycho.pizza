-- reasons 시드 데이터
-- 현재 reason 테이블은 CANCEL / DELETE 케이스의 설명 텍스트만 보관한다.
-- seed world 에서는 audit_log 의 취소/삭제 이벤트에 대응하는 최소 사유를 생성한다.

with cancel_reason_seed as (
    select
        al.workspace_id,
        al.target_id,
        al.event_id,
        case
            when w.name like 'W07%' then '정책 변경과 범위 재조정으로 기존 작업을 유지하지 못해 취소했다.'
            when w.name like 'W10%' then '긴급 수습 우선순위가 반복되며 기존 작업을 중단하고 취소했다.'
            when w.name like 'W12%' then 'workspace 종료 준비 과정에서 잔여 작업을 마무리하지 못해 취소했다.'
            when w.name like 'W06%' then '핸드오프 실패와 외부 협업 지연으로 작업을 계속 진행하지 못했다.'
            else '운영 우선순위 변경으로 작업을 취소했다.'
        end as reason
    from public.audit_log al
    join public.workspaces w
        on w.id = al.workspace_id
    where al.target_type = 'TASK'
      and al.event_type = 'TASK_STATUS_CHANGED'
      and al.to_value = 'CANCELLED'
),
delete_reason_seed as (
    select
        al.workspace_id,
        al.target_id,
        al.event_id,
        '스프린트 범위와 운영 계획에서 제외되어 삭제했다.' as reason
    from public.audit_log al
    where al.event_type = 'TASK_DELETED'
)
insert into public.reasons (
    id,
    created_at,
    updated_at,
    target_id,
    target_type,
    reason,
    event_id,
    event_type,
    workspace_id
)
select
    gen_random_uuid(),
    now(),
    now(),
    s.target_id,
    s.target_type,
    s.reason,
    s.event_id,
    s.event_type,
    s.workspace_id
from (
    select
        crs.workspace_id,
        crs.target_id,
        'TASK' as target_type,
        crs.reason,
        crs.event_id,
        'CANCEL' as event_type
    from cancel_reason_seed crs

    union all

    select
        drs.workspace_id,
        drs.target_id,
        'TASK' as target_type,
        drs.reason,
        drs.event_id,
        'DELETE' as event_type
    from delete_reason_seed drs
) s
where not exists (
    select 1
    from public.reasons r
    where r.event_id = s.event_id
);

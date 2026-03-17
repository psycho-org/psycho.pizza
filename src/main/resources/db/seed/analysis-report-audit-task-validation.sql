-- audit_log 기준 최종 task 스냅샷과 현재 tasks / project_task_mapping 비교
-- 사용 목적:
-- 1) audit 시드가 의도한 최종 status / assignee / due_date / project 와
--    실제 task 시드 결과가 일치하는지 검증
-- 2) 기존 잘못된 시드가 이미 DB에 들어간 상태인지 탐지

with audited_tasks as (
    select distinct
        al.workspace_id,
        al.target_id as task_id
    from public.audit_log al
    where al.target_type = 'TASK'
),
latest_status as (
    select distinct on (al.workspace_id, al.target_id)
        al.workspace_id,
        al.target_id as task_id,
        al.to_value as expected_status,
        al.occurred_at
    from public.audit_log al
    where al.target_type = 'TASK'
      and al.event_type = 'TASK_STATUS_CHANGED'
    order by al.workspace_id, al.target_id, al.occurred_at desc, al.id desc
),
latest_assignee as (
    select distinct on (al.workspace_id, al.target_id)
        al.workspace_id,
        al.target_id as task_id,
        case
            when al.to_value is null or lower(al.to_value) = 'null' then null
            else al.to_value::uuid
        end as expected_assignee_id,
        al.occurred_at
    from public.audit_log al
    where al.target_type = 'TASK'
      and al.event_type = 'TASK_ASSIGNEE_CHANGED'
    order by al.workspace_id, al.target_id, al.occurred_at desc, al.id desc
),
latest_due_date as (
    select distinct on (al.workspace_id, al.target_id)
        al.workspace_id,
        al.target_id as task_id,
        case
            when al.to_value is null or lower(al.to_value) = 'null' then null
            else al.to_value::timestamp
        end as expected_due_date,
        al.occurred_at
    from public.audit_log al
    where al.target_type = 'TASK'
      and al.event_type = 'TASK_DUE_DATE_CHANGED'
    order by al.workspace_id, al.target_id, al.occurred_at desc, al.id desc
),
latest_project as (
    select distinct on (al.workspace_id, al.target_id)
        al.workspace_id,
        al.target_id as task_id,
        al.to_value as expected_project_name,
        al.occurred_at
    from public.audit_log al
    where al.target_type = 'TASK'
      and al.event_type = 'TASK_PROJECT_CHANGED'
    order by al.workspace_id, al.target_id, al.occurred_at desc, al.id desc
),
actual_project_mapping as (
    select
        ptm.workspace_id,
        ptm.task_id,
        count(*) as mapping_count,
        string_agg(p.name, ' | ' order by p.name) as actual_project_names
    from public.project_task_mapping ptm
    join public.projects p
        on p.id = ptm.project_id
       and p.workspace_id = ptm.workspace_id
    group by ptm.workspace_id, ptm.task_id
),
comparison as (
    select
        at.workspace_id,
        t.id as task_id,
        t.title,
        ls.expected_status,
        t.status as actual_status,
        la.expected_assignee_id,
        t.assignee_id as actual_assignee_id,
        ld.expected_due_date,
        t.due_date as actual_due_date,
        lp.expected_project_name,
        apm.actual_project_names,
        apm.mapping_count,
        case
            when ls.task_id is not null and t.status is distinct from ls.expected_status
                then true else false
        end as status_mismatch,
        case
            when la.task_id is not null and t.assignee_id is distinct from la.expected_assignee_id
                then true else false
        end as assignee_mismatch,
        case
            when ld.task_id is not null and t.due_date is distinct from ld.expected_due_date
                then true else false
        end as due_date_mismatch,
        case
            when lp.task_id is not null and (
                apm.mapping_count <> 1
                or apm.actual_project_names is null
                or apm.actual_project_names <> lp.expected_project_name
            )
                then true else false
        end as project_mismatch
    from audited_tasks at
    join public.tasks t
        on t.id = at.task_id
       and t.workspace_id = at.workspace_id
    left join latest_status ls
        on ls.workspace_id = at.workspace_id
       and ls.task_id = at.task_id
    left join latest_assignee la
        on la.workspace_id = at.workspace_id
       and la.task_id = at.task_id
    left join latest_due_date ld
        on ld.workspace_id = at.workspace_id
       and ld.task_id = at.task_id
    left join latest_project lp
        on lp.workspace_id = at.workspace_id
       and lp.task_id = at.task_id
    left join actual_project_mapping apm
        on apm.workspace_id = at.workspace_id
       and apm.task_id = at.task_id
),
mismatch_detail as (
    select
        c.workspace_id,
        c.title,
        'status' as mismatch_type,
        c.expected_status as expected_value,
        c.actual_status as actual_value
    from comparison c
    where c.status_mismatch

    union all

    select
        c.workspace_id,
        c.title,
        'assignee_id' as mismatch_type,
        coalesce(c.expected_assignee_id::text, 'null') as expected_value,
        coalesce(c.actual_assignee_id::text, 'null') as actual_value
    from comparison c
    where c.assignee_mismatch

    union all

    select
        c.workspace_id,
        c.title,
        'due_date' as mismatch_type,
        coalesce(to_char(c.expected_due_date, 'YYYY-MM-DD HH24:MI:SS'), 'null') as expected_value,
        coalesce(to_char(c.actual_due_date, 'YYYY-MM-DD HH24:MI:SS'), 'null') as actual_value
    from comparison c
    where c.due_date_mismatch

    union all

    select
        c.workspace_id,
        c.title,
        'project_mapping' as mismatch_type,
        c.expected_project_name as expected_value,
        coalesce(c.actual_project_names, 'null') as actual_value
    from comparison c
    where c.project_mismatch
)

-- 1) 워크스페이스별 mismatch 요약
select
    md.workspace_id,
    count(*) as mismatch_count
from mismatch_detail md
group by md.workspace_id
order by md.workspace_id;

-- 2) 상세 mismatch 목록
select
    md.workspace_id,
    md.mismatch_type,
    md.title,
    md.expected_value,
    md.actual_value
from mismatch_detail md
order by md.workspace_id, md.title, md.mismatch_type;

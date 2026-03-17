-- analysis_metric_count 시드 데이터
-- audit_log 기준으로 현재 지원하는 analysis event subtype 집계를 적재한다.

with task_sprint_lookup as (
    select distinct
        ptm.workspace_id,
        ptm.task_id,
        spm.sprint_id
    from public.project_task_mapping ptm
    join public.sprint_project_mapping spm
        on spm.workspace_id = ptm.workspace_id
       and spm.project_id = ptm.project_id
),
audit_metric_events as (
    select
        al.workspace_id,
        case
            when al.event_type = 'SPRINT_GOAL_CHANGED' then al.target_id
            when al.event_type = 'SPRINT_PERIOD_CHANGED' then al.target_id
            when al.event_type = 'TASK_ADDED_TO_SPRINT' and nullif(al.to_value, '') is not null then al.to_value::uuid
            when al.event_type = 'TASK_REMOVED_FROM_SPRINT' and nullif(al.from_value, '') is not null then al.from_value::uuid
            else tsl.sprint_id
        end as sprint_id,
        case
            when al.event_type = 'SPRINT_GOAL_CHANGED' then 'GOAL_UPDATED'
            when al.event_type = 'SPRINT_PERIOD_CHANGED' then 'PERIOD_UPDATED'
            when al.event_type = 'TASK_STATUS_CHANGED'
                 and al.from_value = 'DONE'
                 and al.to_value in ('TODO', 'IN_PROGRESS')
                then 'STATUS_REGRESSION_FROM_DONE'
            when al.event_type = 'TASK_STATUS_CHANGED'
                 and al.from_value = 'TODO'
                 and al.to_value = 'DONE'
                then 'TODO_TO_DONE_DIRECT'
            when al.event_type in ('TASK_ADDED_TO_SPRINT', 'TASK_REMOVED_FROM_SPRINT', 'TASK_PROJECT_CHANGED')
                then 'SCOPE_CHURN'
            when al.event_type = 'TASK_STATUS_CHANGED'
                 and al.to_value = 'CANCELLED'
                then 'STATUS_CHANGED_TO_CANCELED'
            else null
        end as event_subtype
    from public.audit_log al
    left join task_sprint_lookup tsl
        on al.target_type = 'TASK'
       and tsl.workspace_id = al.workspace_id
       and tsl.task_id = al.target_id
),
metric_seed as (
    select
        ame.workspace_id,
        ame.sprint_id,
        ame.event_subtype,
        count(*)::int as event_count
    from audit_metric_events ame
    where ame.sprint_id is not null
      and ame.event_subtype is not null
    group by ame.workspace_id, ame.sprint_id, ame.event_subtype
)
insert into public.analysis_metric_count (
    id,
    created_at,
    updated_at,
    workspace_id,
    sprint_id,
    event_subtype,
    count
)
select
    gen_random_uuid(),
    now(),
    now(),
    ms.workspace_id,
    ms.sprint_id,
    ms.event_subtype,
    ms.event_count
from metric_seed ms
where not exists (
    select 1
    from public.analysis_metric_count amc
    where amc.workspace_id = ms.workspace_id
      and amc.sprint_id = ms.sprint_id
      and amc.event_subtype = ms.event_subtype
);

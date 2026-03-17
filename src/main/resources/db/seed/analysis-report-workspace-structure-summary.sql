-- Workspace / Sprint / Project 구조 집계 쿼리
-- 목적:
-- 1) 워크스페이스별 스프린트 수, 프로젝트 수, 태스크 수, 배치된 사용자 수 확인
-- 2) 스프린트별 프로젝트 수 / 태스크 수 / 사용자 수 확인
-- 3) 프로젝트별 태스크 수 / 사용자 수 확인
-- 4) 프로젝트 미배치 backlog task 확인

-- 1) Workspace 요약
with workspace_member_summary as (
    select
        m.workspace_id,
        count(*) as total_memberships,
        count(*) filter (where m.deleted_at is null and m.deleted_by is null) as active_memberships,
        count(*) filter (where m.deleted_at is not null or m.deleted_by is not null) as departed_memberships
    from public.memberships m
    group by m.workspace_id
),
workspace_task_summary as (
    select
        t.workspace_id,
        count(*) as task_count,
        count(distinct t.assignee_id) filter (where t.assignee_id is not null) as assigned_user_count,
        count(*) filter (where t.assignee_id is null) as unassigned_task_count,
        count(*) filter (where ptm.task_id is null) as backlog_task_count
    from public.tasks t
    left join public.project_task_mapping ptm
        on ptm.workspace_id = t.workspace_id
       and ptm.task_id = t.id
    group by t.workspace_id
),
workspace_project_summary as (
    select
        p.workspace_id,
        count(*) as project_count
    from public.projects p
    group by p.workspace_id
),
workspace_sprint_summary as (
    select
        s.workspace_id,
        count(*) as sprint_count
    from public.sprints s
    group by s.workspace_id
)
select
    w.name as workspace_name,
    w.id as workspace_id,
    case when w.deleted_at is null and w.deleted_by is null then 'ACTIVE' else 'CLOSED' end as workspace_state,
    w.deleted_at as workspace_closed_at,
    coalesce(wss.sprint_count, 0) as sprint_count,
    coalesce(wps.project_count, 0) as project_count,
    coalesce(wts.task_count, 0) as task_count,
    coalesce(wms.total_memberships, 0) as total_memberships,
    coalesce(wms.active_memberships, 0) as active_memberships,
    coalesce(wms.departed_memberships, 0) as departed_memberships,
    coalesce(wts.assigned_user_count, 0) as assigned_user_count,
    coalesce(wts.unassigned_task_count, 0) as unassigned_task_count,
    coalesce(wts.backlog_task_count, 0) as backlog_task_count
from public.workspaces w
left join workspace_sprint_summary wss on wss.workspace_id = w.id
left join workspace_project_summary wps on wps.workspace_id = w.id
left join workspace_task_summary wts on wts.workspace_id = w.id
left join workspace_member_summary wms on wms.workspace_id = w.id
order by w.name;

-- 2) Sprint 요약
with sprint_project_counts as (
    select
        spm.workspace_id,
        spm.sprint_id,
        count(distinct spm.project_id) as project_count
    from public.sprint_project_mapping spm
    group by spm.workspace_id, spm.sprint_id
),
sprint_task_counts as (
    select
        spm.workspace_id,
        spm.sprint_id,
        count(distinct ptm.task_id) as task_count,
        count(distinct t.assignee_id) filter (where t.assignee_id is not null) as assigned_user_count
    from public.sprint_project_mapping spm
    left join public.project_task_mapping ptm
        on ptm.workspace_id = spm.workspace_id
       and ptm.project_id = spm.project_id
    left join public.tasks t
        on t.workspace_id = ptm.workspace_id
       and t.id = ptm.task_id
    group by spm.workspace_id, spm.sprint_id
)
select
    w.name as workspace_name,
    s.name as sprint_name,
    s.goal as sprint_goal,
    s.start_date,
    s.end_date,
    coalesce(spc.project_count, 0) as project_count,
    coalesce(stc.task_count, 0) as task_count,
    coalesce(stc.assigned_user_count, 0) as assigned_user_count
from public.sprints s
join public.workspaces w
    on w.id = s.workspace_id
left join sprint_project_counts spc
    on spc.workspace_id = s.workspace_id
   and spc.sprint_id = s.id
left join sprint_task_counts stc
    on stc.workspace_id = s.workspace_id
   and stc.sprint_id = s.id
order by w.name, s.start_date, s.name;

-- 3) Project 요약
select
    w.name as workspace_name,
    s.name as sprint_name,
    p.name as project_name,
    count(distinct ptm.task_id) as task_count,
    count(distinct t.assignee_id) filter (where t.assignee_id is not null) as assigned_user_count,
    count(*) filter (where t.id is not null and t.assignee_id is null) as unassigned_task_count
from public.projects p
join public.workspaces w
    on w.id = p.workspace_id
join public.sprint_project_mapping spm
    on spm.workspace_id = p.workspace_id
   and spm.project_id = p.id
join public.sprints s
    on s.workspace_id = spm.workspace_id
   and s.id = spm.sprint_id
left join public.project_task_mapping ptm
    on ptm.workspace_id = p.workspace_id
   and ptm.project_id = p.id
left join public.tasks t
    on t.workspace_id = ptm.workspace_id
   and t.id = ptm.task_id
group by w.name, s.name, p.name
order by w.name, s.start_date, p.name;

-- 4) Backlog Task 요약
select
    w.name as workspace_name,
    t.title as backlog_task_title,
    t.priority,
    t.status,
    t.due_date,
    t.assignee_id
from public.tasks t
join public.workspaces w
    on w.id = t.workspace_id
left join public.project_task_mapping ptm
    on ptm.workspace_id = t.workspace_id
   and ptm.task_id = t.id
where ptm.task_id is null
order by w.name, t.title;

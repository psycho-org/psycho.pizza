# Workspace 구조 확인 가이드

## Flyway 기준

- 이 seed 문서 묶음은 현재 DB 스키마 기준 `Flyway v8`까지 반영된 상태를 기준으로 작성되었다.
- 특히 `accounts.display_name` 제거(`V8__drop_account_display_name.sql`) 이후 구조를 기준으로 본다.

## 목적

- 워크스페이스별로 몇 개의 `sprint`, `project`, `task`, `membership`이 배치되는지 확인하기 위한 문서다.
- 특히 `W00`처럼 project 미배치 backlog task가 있는 경우도 함께 확인할 수 있어야 한다.
- 특히 `W11`의 membership 이탈, `W12`의 진행 중 workspace 종료가 구조 집계에 어떻게 보이는지도 함께 확인한다.

## 정적 seed 설정 요약

아래 표는 seed 파일에 명시적으로 들어간 정적 정보다.

| Workspace | 시나리오 | Sprint 수 | Project 수 | 계획 membership 수 | lifecycle 메모 |
| --- | --- | ---: | ---: | ---: | --- |
| `W00` | 현재 진행형 레퍼런스 | 3 | 11 | 10 | active sprint 1개 + backlog task 5건 |
| `W01` | 최고점 기준 조직 | 4 | 17 | 15 | 없음 |
| `W02` | 매우 건강한 조직 | 4 | 15 | 14 | 없음 |
| `W03` | 속도 우선 조직 | 5 | 19 | 11 | 없음 |
| `W04` | 품질 우선이나 느린 조직 | 3 | 13 | 13 | 없음 |
| `W05` | 소유권 약화 조직 | 4 | 15 | 10 | 미배정 종료 케이스 포함 |
| `W06` | 협업 단절 조직 | 5 | 19 | 11 | handoff 실패 중심 |
| `W07` | 스코프 변동 과다 조직 | 3 | 12 | 11 | project 이동 포함 |
| `W08` | 장애 반복 조직 | 4 | 15 | 8 | incident 이후 task 유입 포함 |
| `W09` | 운영 혼선 조직 | 5 | 20 | 14 | 미배정 종료 포함 |
| `W10` | 최하점 기준 조직 | 3 | 10 | 6 | 미배정 종료 포함 |
| `W11` | 멤버십 이탈 조직 | 4 | 14 | 9 | 이 중 1명 이탈 예정 |
| `W12` | 진행 중 종료 조직 | 3 | 10 | 7 | workspace 종료 시 전체 membership 종료 |

## 동적 확인 기준

정확한 `project`, `task`, `assigned user` 수는 실제 DB에 seed 적용 후 아래 SQL로 확인한다.

- 쿼리 파일: `src/main/resources/db/seed/analysis-report-workspace-structure-summary.sql`

이 쿼리는 4개의 결과 세트를 반환한다.

1. `workspace 요약`
2. `sprint 요약`
3. `project 요약`
4. `backlog task 요약`

## 결과 세트별 확인 포인트

### 1) Workspace 요약

확인 컬럼:

- `workspace_name`
- `workspace_state`
- `workspace_closed_at`
- `sprint_count`
- `project_count`
- `task_count`
- `total_memberships`
- `active_memberships`
- `departed_memberships`
- `assigned_user_count`
- `unassigned_task_count`
- `backlog_task_count`

특히 봐야 할 값:

- `W11`: `departed_memberships = 1` 이 보여야 한다.
- `W00`: `backlog_task_count = 5` 가 보여야 한다.
- `W12`: `workspace_state = CLOSED` 와 `workspace_closed_at` 이 보여야 한다.
- `W12`: 종료 처리 때문에 `active_memberships = 0` 으로 보이는 것이 자연스럽다.

### 2) Sprint 요약

확인 컬럼:

- `workspace_name`
- `sprint_name`
- `project_count`
- `task_count`
- `assigned_user_count`

이 결과로 각 workspace 안에 sprint가 몇 개 있고,
각 sprint 안에 project와 task가 얼마나 들어가 있는지 확인할 수 있다.

### 3) Project 요약

확인 컬럼:

- `workspace_name`
- `sprint_name`
- `project_name`
- `task_count`
- `assigned_user_count`
- `unassigned_task_count`

이 결과로 각 project 안에 task가 몇 개 있고,
실제로 몇 명의 사용자가 배치되어 있는지 확인할 수 있다.

### 4) Backlog Task 요약

확인 컬럼:

- `workspace_name`
- `backlog_task_title`
- `status`
- `due_date`
- `assignee_id`

이 결과로 어떤 task가 아직 어떤 project에도 배치되지 않은 backlog 상태인지 직접 볼 수 있다.

## lifecycle 케이스 해석

### `W00_Current_In_Progress`

- `workspace_state = ACTIVE` 여야 한다.
- 현재 날짜 기준 진행 중인 sprint가 1개 존재한다.
- `backlog_task_count = 5` 가 보여야 한다.
- backlog task는 `project_task_mapping`에 잡히지 않고 별도 결과 세트에서 확인되어야 한다.

### `W11_Member_Churn`

- `memberships` 한 건이 soft delete 된다.
- 따라서 workspace 요약에서는 `departed_memberships = 1` 이 증가해야 한다.
- 일부 task는 재배정되거나, 미배정 상태로 남을 수 있다.

### `W12_Workspace_Closed_Midflight`

- `workspaces.deleted_at` 이 채워져 `workspace_state = CLOSED` 로 보여야 한다.
- 종료 시점에 `memberships`도 함께 soft delete 되므로 active member 수가 0으로 내려간다.
- 일부 task는 `DONE` 되지 못하고 `IN_PROGRESS`, `TODO`, `CANCELLED` 상태로 남을 수 있다.

## 관련 파일

- `src/main/resources/db/seed/analysis-report-seed-workspaces.sql`
- `src/main/resources/db/seed/analysis-report-seed-memberships.sql`
- `src/main/resources/db/seed/analysis-report-seed-sprints.sql`
- `src/main/resources/db/seed/analysis-report-seed-projects.sql`
- `src/main/resources/db/seed/analysis-report-seed-tasks.sql`
- `src/main/resources/db/seed/analysis-report-seed-lifecycle.sql`
- `src/main/resources/db/seed/analysis-report-workspace-structure-summary.sql`

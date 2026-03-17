# Lifecycle History 정리

## 목적

- `membership` 이탈과 `workspace` 종료처럼 현재 `audit_log` enum으로 직접 표현하기 어려운 lifecycle 이벤트를 별도로 기록한다.
- 실제 DB 반영은 `deleted_at`, `deleted_by` 값을 통해 이루어지고,
  이 문서는 그 타임라인 해석 기준을 제공한다.

## 제약

- 현재 `audit_log`는 `target_type`이 `TASK`, `PROJECT`, `SPRINT`만 가능하다.
- 현재 `event_type`도 `AuditEventType` enum으로 고정되어 있어
  `MEMBERSHIP_LEFT`, `WORKSPACE_CLOSED` 같은 이벤트를 직접 넣지 않는다.
- 따라서 lifecycle 자체는 아래 문서와 `18-analysis-report-seed-lifecycle.sql`로 표현하고,
  audit에는 그 영향(task 지연, 재배정, 취소, 공백 종료)만 남긴다.

## W11_Member_Churn

### 이탈 히스토리

| 시각 | 대상 | 이벤트 | 설명 |
| --- | --- | --- | --- |
| `2026-02-03 18:00:00` | `A024` in `W11` | membership soft delete | 핵심 담당자가 워크스페이스에서 이탈한다. |
| `2026-02-03 18:10:00` 이후 | 관련 task | 후속 영향 발생 | 담당자 재배정, 공백 업무 발생, due date 연장, 상태 회귀가 이어진다. |

### 실제 DB 반영 방식

- `memberships.deleted_at = 2026-02-03 18:00:00`
- `memberships.deleted_by = A001`

### audit에서 읽어야 할 후속 영향

- `W11-S02 P01 - 핵심 담당 이탈 영향 점검`
- `W11-S02 P02 - 공백 업무 우선순위 재설정`
- `W11-S02 P03 - 고객 문의 임시 인계`
- `W11-S04 P02 - 응대 품질 복구`
- `W11-S04 P03 - 잔여 이슈 마감`

해석:

- 이탈 직후 담당자 재배정이 일어나지만,
- 일부 task는 미배정 상태로 종료되거나 due date를 넘겨 완료된다.
- 후반부에도 일정 지연이 이어져 "이탈 여파가 길게 남는다"는 점이 중요하다.

## W12_Workspace_Closed_Midflight

### 종료 히스토리

| 시각 | 대상 | 이벤트 | 설명 |
| --- | --- | --- | --- |
| `2026-02-18 18:30:00` | `W12` | workspace soft delete | 진행 중이던 워크스페이스가 종료된다. |
| `2026-02-18 18:30:00` | all active memberships in `W12` | membership soft delete | 종료와 함께 활성 membership도 함께 닫힌다. |

### 실제 DB 반영 방식

- `workspaces.deleted_at = 2026-02-18 18:30:00`
- `workspaces.deleted_by = A001`
- `memberships.deleted_at = 2026-02-18 18:30:00`
- `memberships.deleted_by = A001`

### audit에서 읽어야 할 후속 영향

- `W12-S02 P04 - 정산 마감 목록 정리`
- `W12-S03 P01 - 종료 공지 발송`
- `W12-S03 P02 - 잔여 정산 처리`
- `W12-S03 P03 - 종료 후 문의 대응 정리`

해석:

- 종료 직전까지 급한 공지/정산 task가 들어오지만,
- 일부는 취소되고 일부는 열린 상태로 남아 종료 시점에 끊긴다.

## 관련 파일

- `src/main/resources/db/seed/18-analysis-report-seed-lifecycle.sql`
- `src/main/resources/db/seed/16-analysis-report-seed-audit-log-w11.sql`
- `src/main/resources/db/seed/17-analysis-report-seed-audit-log-w12.sql`

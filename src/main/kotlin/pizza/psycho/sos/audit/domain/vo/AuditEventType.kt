package pizza.psycho.sos.audit.domain.vo

/*
 * AuditLog에 기록되는 실제 도메인 변경 이벤트
 */

enum class AuditEventType(
    val description: String,
) {
    // ========== MVP-03 ==========
    // -- Sprint --
    SPRINT_GOAL_CHANGED("Sprint Goal 변경"),
    SPRINT_PERIOD_CHANGED("Sprint 기간 변경"),
    TASK_ADDED_TO_SPRINT("Task가 Sprint에 추가됨"),
    TASK_REMOVED_FROM_SPRINT("Task가 Sprint에서 제거됨"),

    // -- Task --
    TASK_STATUS_CHANGED("Task 상태 변경"),
    // ============================

    TASK_ASSIGNEE_CHANGED("Task 담당자 변경"),
    TASK_DUE_DATE_CHANGED("Task 마감일 변경"),
    TASK_PROJECT_CHANGED("Task 프로젝트 변경"),
    TASK_DELETED("Task 삭제"),
}

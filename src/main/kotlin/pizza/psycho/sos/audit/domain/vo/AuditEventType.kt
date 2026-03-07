package pizza.psycho.sos.audit.domain.vo

/*
 * AuditLog에 기록되는 실제 도메인 변경 이벤트
 */

enum class AuditEventType(
    val description: String,
) {
    // -- 🔥Sprint🔥 --
    SPRINT_GOAL_CHANGED("Sprint 목표 변경"),
    /*
     * Sprint Goal 수정
     */

    SPRINT_PERIOD_CHANGED("Sprint 기간 변경"),
    /*
     * Sprint 기간 변경
     */

    TASK_ADDED_TO_SPRINT("Task가 Sprint에 추가됨"),
    TASK_REMOVED_FROM_SPRINT("Task가 Sprint에서 제거됨"),
    /*
     * Sprint 중 Task 추가/삭제
     */

    // -- 🔥Task🔥 --
    TASK_STATUS_CHANGED("Task 상태 변경"),
    /*
     * 리워크 (CSG 이전 상태 회귀)
     * TODO → DONE 직행
     * 특정 상태 장기 방치 (리뷰 병목 포함)
     * Canceled 처리
     * Done → InProgress / Done → Todo
     * InProgress → Todo/Backlog
     */

    TASK_ASSIGNEE_CHANGED("Task 담당자 변경"),
    /*
     * Assignee 변경 빈번
     */

    TASK_DUE_DATE_CHANGED("Task 마감일 변경"),
    /*
     * due date 연장
     */

    TASK_PROJECT_CHANGED("Task 프로젝트 변경"),
    /*
     * - Task 프로젝트 변경
     */

    TASK_DELETED("Task 삭제"),
    /*
     * - Sprint 중 삭제된 Task ≥ 30%
     */
}

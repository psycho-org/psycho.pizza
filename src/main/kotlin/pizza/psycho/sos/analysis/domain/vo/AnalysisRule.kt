package pizza.psycho.sos.analysis.domain.vo

enum class AnalysisRule(
//    val category: AnalysisCategory, // 미정
//    val weight: Int, // 미정
    val description: String,
) {
    ASSIGNEE_MISSING("담당자 미지정 Task 존재"),
    NON_CSG_PROJECT_PRESENT("CSG 외 프로젝트/태스크 존재"),
    CANCELED_DONE_RATIO_HIGH("Canceled 대비 Done 비율 초과"),
    PROJECT_WITHOUT_TASK("프로젝트에 Task 없음"),

    NON_COMMIT_TASK_RATIO_HIGH("커밋 외 Task 비율 초과"),
    LAST_MINUTE_DONE_CONCENTRATION("Sprint 종료 직전 Done 몰림"),
    DUE_DATE_NOT_SET_RATIO_HIGH("Due date 미설정 Task 비율 초과"),

    SPRINT_GOAL_CHANGED("Sprint Goal 수정"),
    TASK_DELETED_IN_SPRINT_RATIO_HIGH("Sprint 중 Task 삭제 비율 초과"),

    DONE_AFTER_DUE_DATE("Due date 이후 완료"),
    DUE_DATE_EXTENDED("Due date 연장"),
    SPRINT_PERIOD_CHANGED("Sprint 기간 변경"),
    TASK_PROJECT_CHANGED("Task 프로젝트 변경"),

    REWORK_DETECTED("리워크 발생"),
    TODO_TO_DONE_DIRECT("TODO → DONE 직행"),
    STATUS_STAGNATION("특정 상태 장기 정체"),
    TASK_CANCELED("Task Canceled 처리"),
    DONE_TO_IN_PROGRESS_OR_TODO("Done 이후 상태 회귀"),
    IN_PROGRESS_TO_TODO("InProgress → Todo/Backlog 회귀"),

    ASSIGNEE_CHANGED_FREQUENTLY("Assignee 변경 빈번"),
    WORKLOAD_CONCENTRATION("특정인 업무 집중"),
    WIP_OVER_LIMIT("WIP 초과"),
    WIP_OVER_LIMIT_DURATION("WIP 초과 상태 지속"),
}

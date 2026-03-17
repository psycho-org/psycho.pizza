package pizza.psycho.sos.audit.domain.vo

/*
 * 로그의 대상 도메인 (무엇이 변경되었는지)
 */
enum class AuditTargetType(
    val description: String,
) {
    TASK("Task"),
    SPRINT("Sprint"),
}

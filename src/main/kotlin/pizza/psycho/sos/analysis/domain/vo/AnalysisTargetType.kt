package pizza.psycho.sos.analysis.domain.vo

enum class AnalysisTargetType(
    val description: String,
) {
    // NOTE: MVP-01에서는 Sprint 분석만 고려합니다.
    SPRINT("Sprint"),
}

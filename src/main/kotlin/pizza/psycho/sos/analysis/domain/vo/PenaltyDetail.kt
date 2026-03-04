package pizza.psycho.sos.analysis.domain.vo

// FIXME: 예시입니다. 수정 예정!
data class PenaltyDetail(
    val ruleId: String,    // 예: "RULE_WIP_LIMIT_EXCEEDED"
    val penalty: Int,      // 룰에 의한 감점 수치
    val evidence: String   // 감점 근거 (예: "담당자가 지정되지 않은 태스크 3건 발견")
)
package pizza.psycho.sos.analysis.domain.vo

// FIXME: 예시입니다. 수정 예정!
data class CategoryPenalty(
    val category: String,  // 예: "WIP", "DUE_DATE", "ASSIGNEE"
    val penalty: Int       // 감점된 점수
)
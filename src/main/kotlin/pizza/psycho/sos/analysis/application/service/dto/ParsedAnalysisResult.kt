package pizza.psycho.sos.analysis.application.service.dto

// FIXME: 다음 PR에서 좀 더 구체화될 예정입니다.
data class ParsedAnalysisResult(
    val summary: String,
    val risks: List<String>,
    val topIssues: List<String>,
    val recommendations: List<String>,
)

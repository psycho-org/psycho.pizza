package pizza.psycho.sos.analysis.application.service.dto

data class ParsedAnalysisResult(
    val summary: String,
    val risks: List<String>,
    val topIssues: List<String>,
    val recommendations: List<String>,
)

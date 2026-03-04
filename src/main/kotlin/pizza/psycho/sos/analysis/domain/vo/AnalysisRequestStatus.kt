package pizza.psycho.sos.analysis.domain.vo

enum class AnalysisRequestStatus(val description: String) {
    QUEUED("분석 요청 생성됨"),
    RUNNING("분석 실행 중"),
    DONE("분석 완료"),
    FAILED("분석 실패")
}
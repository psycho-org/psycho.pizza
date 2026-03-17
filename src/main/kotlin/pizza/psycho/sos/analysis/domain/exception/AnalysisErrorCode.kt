package pizza.psycho.sos.analysis.domain.exception

import org.springframework.http.HttpStatus
import pizza.psycho.sos.common.exception.BaseErrorCode

enum class AnalysisErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    ANALYSIS_REPORT_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "분석 리포트가 없습니다.",
    ),
    INVALID_ANALYSIS_STATE(
        HttpStatus.NOT_FOUND,
        "유효하지 않은 분석 요청 상태입니다.",
    ),
    SPRINT_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "분석할 스프린트가 없습니다.",
    ),
    ANALYSIS_REQUEST_ID_NOT_GENERATED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "저장 후 ID가 생성되어야 합니다.",
    ),
    ANALYSIS_REQUEST_CREATED_AT_NOT_GENERATED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "저장 후 생성 시간이 기록되어야 합니다.",
    ),
    ANALYSIS_JOB_QUEUE_FULL(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Analysis queue is full",
    ),
    ANALYSIS_REQUEST_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "해당 분석 요청 정보를 찾을 수 없거나 식별자가 존재하지 않습니다.",
    ),

    // 릴레이 서버 연동 관련 에러
    RELAY_SERVER_BAD_REQUEST(
        HttpStatus.BAD_REQUEST,
        "릴레이 서버로 잘못된 분석 요청이 전달되었습니다.",
    ),
    RELAY_SERVER_INTERNAL_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "릴레이 서버 내부에서 오류가 발생했습니다.",
    ),
    RELAY_SERVER_CONNECTION_FAILED(
        HttpStatus.SERVICE_UNAVAILABLE,
        "릴레이 서버와 연결할 수 없거나 응답이 없습니다.",
    ),
    ;

    override val code: String = name
}

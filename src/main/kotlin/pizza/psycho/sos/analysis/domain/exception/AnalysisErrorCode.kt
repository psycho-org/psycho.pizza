package pizza.psycho.sos.analysis.domain.exception

import org.springframework.http.HttpStatus
import pizza.psycho.sos.common.exception.BaseErrorCode

enum class AnalysisErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
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

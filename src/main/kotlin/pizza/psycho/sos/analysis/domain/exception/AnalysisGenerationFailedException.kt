package pizza.psycho.sos.analysis.domain.exception

import pizza.psycho.sos.common.handler.DomainException

class AnalysisGenerationFailedException(
    message: String = "외부 AI 서비스를 통한 분석 리포트 생성에 실패했습니다.",
    cause: Throwable? = null,
) : DomainException(message, cause)

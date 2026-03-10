package pizza.psycho.sos.analysis.domain.exception

import pizza.psycho.sos.common.handler.DomainException

class LlmResponseParsingException(
    message: String = "LLM 응답(JSON)을 파싱하는 데 실패했습니다.",
    cause: Throwable? = null,
) : DomainException(message, cause)

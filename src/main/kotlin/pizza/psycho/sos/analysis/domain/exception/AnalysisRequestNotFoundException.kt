package pizza.psycho.sos.analysis.domain.exception

import pizza.psycho.sos.common.handler.DomainException

class AnalysisRequestNotFoundException(
    message: String = "AnalysisRequest ID가 존재하지 않습니다.",
) : DomainException(message)

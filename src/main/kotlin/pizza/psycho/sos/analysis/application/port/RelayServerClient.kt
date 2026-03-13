package pizza.psycho.sos.analysis.application.port

import pizza.psycho.sos.analysis.application.service.dto.SprintAnalysisPayload
import java.util.UUID

/*
 * RelayServerPort
 * - 릴레이 서버 요청 인터페이스
 */
interface RelayServerClient {
    fun send(
        jobId: UUID,
        workspaceId: UUID,
        payload: SprintAnalysisPayload,
    )
}

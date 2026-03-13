package pizza.psycho.sos.analysis.infrastructure.llm

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import pizza.psycho.sos.analysis.application.port.RelayServerClient
import pizza.psycho.sos.analysis.application.service.dto.SprintAnalysisPayload
import pizza.psycho.sos.analysis.domain.exception.AnalysisErrorCode
import pizza.psycho.sos.analysis.infrastructure.llm.dto.RelayAnalysisRequest
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.support.log.loggerDelegate
import java.util.UUID

@Component
class RelayServerAdapter(
    @param:Qualifier("relayServerRestClient") private val restClient: RestClient,
) : RelayServerClient {
    private val log by loggerDelegate()

    override fun send(
        jobId: UUID,
        workspaceId: UUID,
        payload: SprintAnalysisPayload,
    ) {
        val requestBody =
            RelayAnalysisRequest(
                jobId = jobId,
                workspaceId = workspaceId,
                data = payload,
            )

        try {
            restClient
                .post()
                .uri("/api/v1/analyze")
                .body(requestBody)
                .retrieve()
                .onStatus({ it.is4xxClientError }) { _, response ->
                    log.error("❌ Relay Server 4xx Error: ${response.statusCode}")
                    throw DomainException(AnalysisErrorCode.RELAY_SERVER_BAD_REQUEST)
                }.onStatus({ it.is5xxServerError }) { _, response ->
                    log.error("❌ Relay Server 5xx Error: ${response.statusCode}")
                    throw DomainException(AnalysisErrorCode.RELAY_SERVER_INTERNAL_ERROR)
                }.toBodilessEntity() // 응답 바디는 무시하고 성공(200 OK) 여부만 확인
        } catch (e: RestClientException) {
            // 3. 네트워크 타임아웃, 서버 다운 등 아예 연결이 안 될 때의 예외 처리
            log.error("❌ Relay Server Connection Failed (JobId: $jobId)", e)
            throw DomainException(AnalysisErrorCode.RELAY_SERVER_CONNECTION_FAILED, cause = e)
        }
    }
}

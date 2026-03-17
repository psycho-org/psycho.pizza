package pizza.psycho.sos.analysis.application.service.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class BusinessNotifyPayload(
    @param:JsonProperty("job_id")
    val jobId: Int,
    @param:JsonProperty("external_request_id")
    val externalRequestId: String,
    @param:JsonProperty("openai_response_id")
    val openaiResponseId: String?,
    @param:JsonProperty("openai_state")
    val openaiState: String,
    @param:JsonProperty("postprocess_state")
    val postprocessState: String,
    val result: ResultPayload,
    val error: Map<String, Any>?,
    @param:JsonProperty("occurred_at")
    val occurredAt: Instant,
) {
    data class ResultPayload(
        val analysis: String,
    )
}

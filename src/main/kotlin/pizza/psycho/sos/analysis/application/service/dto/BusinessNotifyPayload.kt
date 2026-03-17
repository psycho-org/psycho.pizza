package pizza.psycho.sos.analysis.application.service.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class BusinessNotifyPayload(
    @JsonProperty("job_id")
    val jobId: Long,
    @JsonProperty("external_request_id")
    val externalRequestId: String,
    @JsonProperty("openai_response_id")
    val openaiResponseId: String?,
    @JsonProperty("openai_state")
    val openaiState: String,
    @JsonProperty("postprocess_state")
    val postprocessState: String,
    val result: String,
    val error: Map<String, Any>?,
    @JsonProperty("occurred_at")
    val occurredAt: LocalDateTime,
)

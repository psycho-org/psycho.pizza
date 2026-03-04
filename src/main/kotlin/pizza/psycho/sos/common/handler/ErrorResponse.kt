package pizza.psycho.sos.common.handler

import pizza.psycho.sos.common.response.ApiResponse
import java.time.Instant

data class ErrorResponse(
    override val timestamp: Instant,
    override val status: Int,
    override val message: String,
    val error: String,
    val code: String? = null,
    val path: String,
    val details: Map<String, List<String>>? = null,
) : ApiResponse<Nothing>

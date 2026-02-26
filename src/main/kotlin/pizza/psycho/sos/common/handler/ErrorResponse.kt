package pizza.psycho.sos.common.handler

import java.time.Instant

data class ErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val details: Map<String, List<String>>? = null,
)

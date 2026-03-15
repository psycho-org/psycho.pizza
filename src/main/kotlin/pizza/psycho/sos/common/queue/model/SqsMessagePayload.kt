package pizza.psycho.sos.common.queue.model

data class SqsMessagePayload(
    val groupId: String,
    val deduplicationId: String = "$groupId-${System.currentTimeMillis()}",
    val body: Map<String, Any> = emptyMap(),
)

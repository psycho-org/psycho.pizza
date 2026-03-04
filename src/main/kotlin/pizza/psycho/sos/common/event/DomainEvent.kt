package pizza.psycho.sos.common.event

import java.time.Instant

interface DomainEvent {
    val occurredAt: Instant
    val sourceId: String? get() = null
    val eventType: String get() = this::class.simpleName ?: "UnknownEvent"
}

package pizza.psycho.sos.common.event

open class DomainEventDelegate : AggregateRoot {
    private val events = mutableSetOf<DomainEvent>()

    override fun registerEvent(event: DomainEvent) {
        events += event
    }

    override fun domainEvents(): List<DomainEvent> = events.toList()

    override fun pullDomainEvents(): List<DomainEvent> = events.toList().also { events.clear() }
}

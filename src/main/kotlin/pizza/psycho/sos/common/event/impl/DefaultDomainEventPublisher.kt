package pizza.psycho.sos.common.event.impl

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import pizza.psycho.sos.common.event.DomainEvent
import pizza.psycho.sos.common.event.DomainEventPublisher

@Component
class DefaultDomainEventPublisher(
    private val publisher: ApplicationEventPublisher,
) : DomainEventPublisher {
    override fun <T : DomainEvent> publish(event: T) {
        publisher.publishEvent(event)
    }
}

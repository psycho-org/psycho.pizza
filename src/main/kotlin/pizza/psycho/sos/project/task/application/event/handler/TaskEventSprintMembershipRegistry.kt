package pizza.psycho.sos.project.task.application.event.handler

import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class TaskEventSprintMembershipRegistry {
    private val sprintMembershipByEventId = ConcurrentHashMap<UUID, Boolean>()

    fun register(
        eventId: UUID,
        wasInActiveSprint: Boolean,
    ) {
        sprintMembershipByEventId[eventId] = wasInActiveSprint
    }

    fun consume(eventId: UUID): Boolean? = sprintMembershipByEventId.remove(eventId)

    fun clear(eventId: UUID) {
        sprintMembershipByEventId.remove(eventId)
    }
}

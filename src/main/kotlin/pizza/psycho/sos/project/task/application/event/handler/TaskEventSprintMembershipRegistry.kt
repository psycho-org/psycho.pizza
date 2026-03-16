package pizza.psycho.sos.project.task.application.event.handler

import org.springframework.stereotype.Component
import pizza.psycho.sos.project.sprint.application.port.out.dto.SprintPeriodSnapshot
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class TaskEventSprintMembershipRegistry {
    private val sprintMembershipByEventId = ConcurrentHashMap<UUID, List<SprintPeriodSnapshot>>()

    fun register(
        eventId: UUID,
        sprintPeriods: List<SprintPeriodSnapshot>,
    ) {
        sprintMembershipByEventId[eventId] = sprintPeriods
    }

    fun consume(eventId: UUID): List<SprintPeriodSnapshot>? = sprintMembershipByEventId.remove(eventId)

    fun clear(eventId: UUID) {
        sprintMembershipByEventId.remove(eventId)
    }
}

package pizza.psycho.sos.project.reason.domain.repository

import pizza.psycho.sos.project.reason.domain.model.entity.Reason
import java.util.UUID

interface ReasonRepository {
    fun existsByEventId(eventId: UUID): Boolean

    fun save(reason: Reason): Reason
}

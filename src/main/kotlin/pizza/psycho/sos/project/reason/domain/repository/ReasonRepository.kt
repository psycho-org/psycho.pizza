package pizza.psycho.sos.project.reason.domain.repository

import pizza.psycho.sos.project.reason.domain.model.entity.Reason

interface ReasonRepository {
    fun save(reason: Reason): Reason
}

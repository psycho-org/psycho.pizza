package pizza.psycho.sos.project.task.domain.model.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
data class AssigneeId(
    @Column(name = "assignee_id", unique = false, nullable = true)
    var value: UUID? = null,
) {
    companion object {
        fun empty() = AssigneeId()
    }
}

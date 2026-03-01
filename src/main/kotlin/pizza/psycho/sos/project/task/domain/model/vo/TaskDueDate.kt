package pizza.psycho.sos.project.task.domain.model.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.Instant

@Embeddable
data class TaskDueDate(
    @Column(name = "due_date", nullable = true)
    var value: Instant? = null,
) {
    init {
        value?.let {
            require(it.isAfter(Instant.now())) { "due date must be after ${Instant.now()}" }
        }
    }
}

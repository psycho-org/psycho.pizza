package pizza.psycho.sos.project.task.domain.model.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import pizza.psycho.sos.project.task.domain.exception.InvalidDueDateException
import java.time.Instant

@Embeddable
data class TaskDueDate(
    @Column(name = "due_date", nullable = true)
    var value: Instant? = null,
) {
    companion object {
        fun withValidation(value: Instant? = null): TaskDueDate {
            value?.let {
                if (!it.isAfter(Instant.now())) {
                    throw InvalidDueDateException("Due date must be after current time")
                }
            }

            return TaskDueDate(value)
        }
    }
}

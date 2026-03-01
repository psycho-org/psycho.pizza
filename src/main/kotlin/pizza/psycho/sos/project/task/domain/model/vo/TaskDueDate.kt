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
                if (!it.isAfter(Instant.now())) throw InvalidDueDateException("마감일은 현재 시간 이후여야 합니다")
            }

            return TaskDueDate(value)
        }
    }
}

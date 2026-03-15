package pizza.psycho.sos.project.task.application.port.out.dto

import java.util.UUID

class SprintTaskMembershipSnapshot private constructor(
    private val taskIds: Set<UUID>,
) {
    fun contains(taskId: UUID): Boolean = taskIds.contains(taskId)

    companion object {
        fun of(taskIds: Collection<UUID>): SprintTaskMembershipSnapshot = SprintTaskMembershipSnapshot(taskIds.toSet())
    }

    override fun equals(other: Any?): Boolean = this === other || (other is SprintTaskMembershipSnapshot && taskIds == other.taskIds)

    override fun hashCode(): Int = taskIds.hashCode()
}

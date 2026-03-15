package pizza.psycho.sos.project.project.application.port.out

import pizza.psycho.sos.project.sprint.application.port.out.dto.SprintPeriodSnapshot
import java.util.UUID

/**
 * Project 가 어느 활성 스프린트에도 속해 있는지 여부를 조회하기 위한 읽기용 포트.
 */
interface ProjectSprintParticipationQuery {
    fun existsActiveSprintByProjectId(
        projectId: UUID,
        workspaceId: UUID,
    ): Boolean

    fun findActiveSprintPeriodsByProjectId(
        projectId: UUID,
        workspaceId: UUID,
    ): List<SprintPeriodSnapshot>

    fun findActiveSprintIdsByProjectId(
        projectId: UUID,
        workspaceId: UUID,
    ): List<UUID>
}

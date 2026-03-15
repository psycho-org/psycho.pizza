package pizza.psycho.sos.project.sprint.domain.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import java.util.UUID

interface SprintRepository {
    fun findActiveSprintByIdOrNull(
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): Sprint?

    /**
     * 주어진 Task가 어느 활성 스프린트에도 속해 있는지 여부를 확인한다.
     */
    fun existsActiveSprintByTaskId(
        taskId: UUID,
        workspaceId: WorkspaceId,
    ): Boolean

    /**
     * 주어진 Task가 속한 활성 스프린트 ID 목록을 조회한다.
     */
    fun findActiveSprintIdsByTaskId(
        taskId: UUID,
        workspaceId: WorkspaceId,
    ): List<UUID>

    /**
     * 주어진 Project가 어느 활성 스프린트에도 속해 있는지 여부를 확인한다.
     */
    fun existsActiveSprintByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): Boolean

    /**
     * 주어진 Project가 속한 활성 스프린트 ID 목록을 조회한다.
     */
    fun findActiveSprintIdsByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): List<UUID>

    /**
     * 주어진 Project가 속한 활성 스프린트 목록을 조회한다.
     */
    fun findActiveSprintsByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): List<Sprint>

    fun existsActiveSprintByTaskIdAndSprintId(
        taskId: UUID,
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): Boolean

    fun findActiveSprintsByTaskId(
        taskId: UUID,
        workspaceId: WorkspaceId,
    ): List<Sprint>

    fun deleteById(
        sprintId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int

    fun save(sprint: Sprint): Sprint

    fun findActiveSprints(
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Sprint>
}

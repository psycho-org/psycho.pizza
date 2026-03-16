package pizza.psycho.sos.project.task.application.port.out

import java.util.UUID

/**
 * Task 가 어느 활성 스프린트에도 속해 있는지 여부를 조회하기 위한 읽기용 포트.
 *
 * task 모듈은 sprint 도메인의 구현 세부사항을 몰라도 되고,
 * 단순히 이 포트를 통해 활성 스프린트 소속 여부만 확인한다.
 */
interface TaskSprintParticipationQuery {
    fun existsActiveSprintByTaskId(
        taskId: UUID,
        workspaceId: UUID,
    ): Boolean

    fun findTaskIdsInActiveSprints(
        taskIds: Collection<UUID>,
        workspaceId: UUID,
    ): Set<UUID>
}

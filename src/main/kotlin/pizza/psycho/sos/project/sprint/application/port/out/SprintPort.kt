package pizza.psycho.sos.project.sprint.application.port.out

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.application.port.out.dto.SprintSnapshot
import java.util.UUID

/**
 * 다른 모듈에서 sprint 정보를 읽기 위해 사용하는 읽기용 포트.
 */
interface SprintPort {
    fun findByIdWithProjects(
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): SprintSnapshot?
}

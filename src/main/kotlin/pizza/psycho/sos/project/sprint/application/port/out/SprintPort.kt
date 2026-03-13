package pizza.psycho.sos.project.sprint.application.port.out

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.application.port.out.dto.SprintSnapshot
import java.util.UUID

/**
 * Sprint 모듈에 대한 읽기/쓰기용 포트.
 *
 * 다른 애플리케이션 서비스에서 Sprint 정보를 조회하거나 조작할 때
 * Sprint 도메인/인프라에 직접 의존하지 않고 이 인터페이스를 통해 접근한다.
 */
interface SprintPort {
    /**
     * 주어진 ID와 워크스페이스에 해당하는 Sprint 정보를 조회한다.
     *
     * Sprint 기본 정보와 해당 Sprint에 속한 Project ID 목록을 함께 반환한다.
     * Sprint 가 없으면 null 을 반환한다.
     */
    fun findByIdWithProjects(
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): SprintSnapshot?
}

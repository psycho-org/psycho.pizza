package pizza.psycho.sos.analysis.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import pizza.psycho.sos.analysis.domain.entity.AnalysisMetricCount
import pizza.psycho.sos.analysis.domain.vo.AnalysisEventSubtype
import java.util.UUID

interface AnalysisMetricCountRepository : JpaRepository<AnalysisMetricCount, UUID> {
    fun findByWorkspaceIdAndSprintIdAndEventSubtype(
        workspaceId: UUID,
        sprintId: UUID,
        eventSubtype: AnalysisEventSubtype,
    ): AnalysisMetricCount?

    fun findAllByWorkspaceIdAndSprintId(
        workspaceId: UUID,
        sprintId: UUID,
    ): List<AnalysisMetricCount>
}

package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.analysis.domain.entity.AnalysisMetricCount
import pizza.psycho.sos.analysis.domain.vo.AnalysisEventSubtype
import pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisMetricCountRepository
import java.util.UUID

@Service
class AnalysisMetricCountService(
    private val analysisMetricCountRepository: AnalysisMetricCountRepository,
) {
    @Transactional
    fun increase(
        workspaceId: UUID,
        sprintId: UUID,
        eventSubtype: AnalysisEventSubtype,
        delta: Int = 1,
    ) {
        val metric =
            analysisMetricCountRepository.findByWorkspaceIdAndSprintIdAndEventSubtype(
                workspaceId = workspaceId,
                sprintId = sprintId,
                eventSubtype = eventSubtype,
            ) ?: AnalysisMetricCount(
                workspaceId = workspaceId,
                sprintId = sprintId,
                eventSubtype = eventSubtype,
                count = 0,
            )

        metric.increase(delta)
        analysisMetricCountRepository.save(metric)
    }

    @Transactional(readOnly = true)
    fun getCounts(
        workspaceId: UUID,
        sprintId: UUID,
    ): Map<AnalysisEventSubtype, Int> =
        analysisMetricCountRepository
            .findAllByWorkspaceIdAndSprintId(
                workspaceId = workspaceId,
                sprintId = sprintId,
            ).associate { it.eventSubtype to it.count }
}

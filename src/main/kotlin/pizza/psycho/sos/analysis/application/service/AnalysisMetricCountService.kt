package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.analysis.domain.entity.AnalysisMetricCount
import pizza.psycho.sos.analysis.domain.vo.AnalysisMetricKey
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
        metricKey: AnalysisMetricKey,
        delta: Int = 1,
    ) {
        val metric =
            analysisMetricCountRepository.findByWorkspaceIdAndSprintIdAndMetricKey(
                workspaceId = workspaceId,
                sprintId = sprintId,
                metricKey = metricKey,
            ) ?: AnalysisMetricCount(
                workspaceId = workspaceId,
                sprintId = sprintId,
                metricKey = metricKey,
                metricCount = 0,
            )

        metric.increase(delta)
        analysisMetricCountRepository.save(metric)
    }

    @Transactional(readOnly = true)
    fun getCounts(
        workspaceId: UUID,
        sprintId: UUID,
    ): Map<AnalysisMetricKey, Int> =
        analysisMetricCountRepository
            .findAllByWorkspaceIdAndSprintId(
                workspaceId = workspaceId,
                sprintId = sprintId,
            ).associate { it.metricKey to it.metricCount }
}

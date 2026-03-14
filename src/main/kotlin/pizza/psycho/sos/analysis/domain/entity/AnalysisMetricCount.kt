package pizza.psycho.sos.analysis.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import pizza.psycho.sos.analysis.domain.vo.AnalysisMetricKey
import pizza.psycho.sos.common.entity.BaseEntity
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "analysis_metric_count",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_analysis_metric_count",
            columnNames = ["workspace_id", "sprint_id", "metric_key"],
        ),
    ],
)
class AnalysisMetricCount(
    @Column(name = "workspace_id", nullable = false)
    val workspaceId: UUID,
    @Column(name = "sprint_id", nullable = false)
    val sprintId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_key", nullable = false, length = 100)
    val metricKey: AnalysisMetricKey,
    @Column(name = "metric_count", nullable = false)
    var metricCount: Int = 0,
) : BaseEntity() {
    fun increase(delta: Int = 1) {
        // TODO: domain exception
        require(delta > 0) { "delta must be positive" }
        metricCount += delta
        updatedAt = Instant.now()
    }
}

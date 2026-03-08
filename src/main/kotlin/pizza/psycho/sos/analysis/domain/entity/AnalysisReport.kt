package pizza.psycho.sos.analysis.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import pizza.psycho.sos.analysis.domain.vo.AnalysisTargetType
import pizza.psycho.sos.common.entity.BaseDeletableEntity
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "analysis_report")
class AnalysisReport(
    @Column(name = "analysis_request_id", nullable = false, updatable = false)
    val analysisRequestId: UUID,
    @Column(name = "workspace_id", nullable = false, updatable = false)
    val workspaceId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50, updatable = false)
    val targetType: AnalysisTargetType,
    @Column(name = "target_id", nullable = false, updatable = false)
    val targetId: UUID,
    @Column(name = "score_total", nullable = false, updatable = false)
    val scoreTotal: Int,
    @Column(name = "score_version", nullable = false, length = 50, updatable = false)
    val scoreVersion: String,
    // TODO: vo 지정 필요할 수도 있음
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_penalties", nullable = false, columnDefinition = "jsonb", updatable = false)
    val categoryPenalties: String,
    // TODO: vo 지정 필요할 수도 있음
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "penalty_details", nullable = false, columnDefinition = "jsonb", updatable = false)
    val penaltyDetails: String,
    @Column(name = "generated_at", nullable = false, updatable = false)
    var generatedAt: Instant = Instant.now(),
) : BaseDeletableEntity() {
    @Column(name = "ai_insight", columnDefinition = "TEXT")
    var aiInsight: String? = null
        private set

    @Column(name = "run_id", length = 100)
    var runId: String? = null
        private set

    fun attachAiInsight(text: String) {
        this.aiInsight = text
    }

    fun attachRunId(runId: String) {
        this.runId = runId
    }
}

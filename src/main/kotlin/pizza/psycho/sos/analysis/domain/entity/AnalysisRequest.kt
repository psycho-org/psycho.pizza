package pizza.psycho.sos.analysis.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import pizza.psycho.sos.analysis.domain.exception.AnalysisErrorCode
import pizza.psycho.sos.analysis.domain.vo.AnalysisRequestStatus
import pizza.psycho.sos.analysis.domain.vo.AnalysisTargetType
import pizza.psycho.sos.common.entity.BaseEntity
import pizza.psycho.sos.common.handler.DomainException
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "analysis_request")
class AnalysisRequest(
    @Column(name = "workspace_id", nullable = false, updatable = false)
    val workspaceId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50, updatable = false)
    val targetType: AnalysisTargetType,
    @Column(name = "target_id", nullable = false, updatable = false)
    val targetId: UUID,
    @Column(name = "requested_by", nullable = true, updatable = false)
    val requestedBy: UUID? = null,
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: AnalysisRequestStatus = AnalysisRequestStatus.QUEUED
        protected set

    @Column(name = "started_at", nullable = true)
    var startedAt: Instant? = null
        protected set

    @Column(name = "completed_at", nullable = true)
    var completedAt: Instant? = null
        protected set

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null
        protected set

    /**
     * QUEUED -> RUNNING
     * - startedAt을 기록하고
     * - completedAt/errorMessage는 초기화
     */
    fun markAsRunning() {
        if (status != AnalysisRequestStatus.QUEUED) {
            throw DomainException(
                AnalysisErrorCode.INVALID_ANALYSIS_STATE,
                "분석 요청 상태가 QUEUED일 때만 RUNNING으로 변경할 수 있습니다. (현재 상태=$status)",
            )
        }
        status = AnalysisRequestStatus.RUNNING
        startedAt = Instant.now()
        completedAt = null
        errorMessage = null
    }

    /**
     * RUNNING -> DONE
     * - completedAt 기록
     */
    fun markAsDone() {
        if (status != AnalysisRequestStatus.RUNNING) {
            throw DomainException(
                AnalysisErrorCode.INVALID_ANALYSIS_STATE,
                "분석 요청 상태가 RUNNING일 때만 DONE으로 변경할 수 있습니다. (현재 상태=$status)",
            )
        }
        status = AnalysisRequestStatus.DONE
        completedAt = Instant.now()
    }

    /**
     * RUNNING -> FAILED
     * - completedAt 기록 + errorMessage 저장
     */
    fun markAsFailed(reason: String) {
        if (status != AnalysisRequestStatus.RUNNING) {
            throw DomainException(
                AnalysisErrorCode.INVALID_ANALYSIS_STATE,
                "분석 요청 상태가 RUNNING일 때만 FAILED로 변경할 수 있습니다. (현재 상태=$status)",
            )
        }
        status = AnalysisRequestStatus.FAILED
        completedAt = Instant.now()
        errorMessage = reason
    }

    /*
     * RUNNING -> QUEUED
     * - 작업 진행 중 서버 종료된 경우 QUEUED로 복구
     * - startedAt 초기화
     */
    fun markAsQueuedForRetry() {
        if (status != AnalysisRequestStatus.RUNNING) {
            throw DomainException(
                AnalysisErrorCode.INVALID_ANALYSIS_STATE,
                "분석 요청 상태가 RUNNING일 때만 QUEUED로 변경할 수 있습니다. (현재 상태=$status)",
            )
        }
        status = AnalysisRequestStatus.QUEUED
        startedAt = null
    }

    companion object {
        fun create(
            workspaceId: UUID,
            sprintId: UUID,
            memberId: UUID,
        ): AnalysisRequest =
            AnalysisRequest(
                workspaceId = workspaceId,
                targetType = AnalysisTargetType.SPRINT,
                targetId = sprintId,
                requestedBy = memberId,
            )
    }
}

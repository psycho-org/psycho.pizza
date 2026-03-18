package pizza.psycho.sos.analysis.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pizza.psycho.sos.analysis.domain.entity.AnalysisRequest
import pizza.psycho.sos.analysis.domain.vo.AnalysisRequestStatus
import java.time.Instant
import java.util.UUID

@Repository
interface AnalysisRequestRepository : JpaRepository<AnalysisRequest, UUID> {
    fun findAllByStatus(status: AnalysisRequestStatus): List<AnalysisRequest>

    /*
     * analysis_request 테이블에서
     * 특정 workspace_id, 특정 target_id (= sprintId)에 해당하는 요청 목록을 가져오고,
     * 각 요청마다 “이 요청에 연결된 report가 있고, 그 report의 ai_insight가 null이 아닌가?”를 계산해서 true / false로 같이 반환하는 쿼리
     */
    @Query(
        """
        select new pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisRequestListItemRow(
            ar.id,
            ar.status,
            case
                when exists (
                    select 1
                    from AnalysisReport rep
                    where rep.analysisRequestId = ar.id
                      and rep.aiInsight is not null
                )
                then true
                else false
            end,
            ar.createdAt
        )
        from AnalysisRequest ar
        where ar.workspaceId = :workspaceId
          and ar.targetId = :sprintId
        order by ar.createdAt desc
        """,
    )
    fun findAnalysisRequestListItems(
        @Param("workspaceId") workspaceId: UUID,
        @Param("sprintId") sprintId: UUID,
    ): List<AnalysisRequestListItemRow>
}

data class AnalysisRequestListItemRow(
    val analysisRequestId: UUID,
    val status: AnalysisRequestStatus,
    val hasReport: Boolean,
    val requestedAt: Instant,
)

package pizza.psycho.sos.analysis.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pizza.psycho.sos.analysis.domain.entity.AnalysisRequest
import java.util.UUID

@Repository
interface AnalysisRequestRepository : JpaRepository<AnalysisRequest, UUID> {}
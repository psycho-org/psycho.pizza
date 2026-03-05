package pizza.psycho.sos.audit.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pizza.psycho.sos.analysis.domain.entity.AnalysisReport
import java.util.UUID

@Repository
interface AuditLogRepository : JpaRepository<AnalysisReport, UUID>

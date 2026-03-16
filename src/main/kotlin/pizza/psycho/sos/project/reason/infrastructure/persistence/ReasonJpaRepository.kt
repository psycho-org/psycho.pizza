package pizza.psycho.sos.project.reason.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import pizza.psycho.sos.project.reason.domain.model.entity.Reason
import pizza.psycho.sos.project.reason.domain.repository.ReasonRepository
import java.util.UUID

@Component
interface ReasonJpaRepository :
    ReasonRepository,
    JpaRepository<Reason, UUID>

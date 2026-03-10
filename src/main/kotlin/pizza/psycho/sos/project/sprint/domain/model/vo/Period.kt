package pizza.psycho.sos.project.sprint.domain.model.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import pizza.psycho.sos.common.handler.DomainException
import java.time.Instant

@Embeddable
data class Period(
    @Column(name = "start_date", nullable = false)
    var startDate: Instant,
    @Column(name = "end_date", nullable = false)
    var endDate: Instant,
) {
    init {
        if (!startDate.isBefore(endDate)) {
            throw DomainException("startDate cannot be after endDate")
        }
    }

    protected constructor() : this(Instant.EPOCH, Instant.EPOCH.plusSeconds(1))
}

package pizza.psycho.sos.project.sprint.domain.model.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.project.sprint.domain.exception.SprintErrorCode.INVALID_DATE_RANGE
import java.time.Instant
import java.time.temporal.ChronoUnit

@Embeddable
data class Period(
    @Column(name = "start_date", nullable = false)
    val startDate: Instant,
    @Column(name = "end_date", nullable = false)
    val endDate: Instant,
) {
    init {
        if (!startDate.isBefore(endDate)) {
            throw DomainException(INVALID_DATE_RANGE, "startDate cannot be after endDate")
        }

        val days = ChronoUnit.DAYS.between(startDate, endDate)
        if (!validate(days)) {
            throw DomainException(INVALID_DATE_RANGE, "period must be at least 7 days and at most 28 days")
        }
    }

    private fun validate(days: Long) = days in 7..28

    protected constructor() : this(Instant.EPOCH, Instant.EPOCH.plusSeconds(MIN_VALID_DURATION_SECONDS))

    companion object {
        private const val MIN_VALID_DURATION_SECONDS: Long = 60 * 60 * 24 * 7
    }
}

package pizza.psycho.sos.project.sprint.domain.model.vo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import pizza.psycho.sos.common.handler.DomainException
import java.time.Instant

class PeriodTests {
    @Test
    fun `시작일이 종료일보다 빠르면 Period가 생성된다`() {
        val start = Instant.parse("2026-02-01T00:00:00Z")
        val end = Instant.parse("2026-02-10T00:00:00Z")

        val period = Period(start, end)

        assertEquals(start, period.startDate)
        assertEquals(end, period.endDate)
    }

    @Test
    fun `시작일이 종료일 이후면 DomainException이 발생한다`() {
        val start = Instant.parse("2026-02-10T00:00:00Z")
        val end = Instant.parse("2026-02-01T00:00:00Z")

        assertThrows(DomainException::class.java) {
            Period(start, end)
        }
    }
}

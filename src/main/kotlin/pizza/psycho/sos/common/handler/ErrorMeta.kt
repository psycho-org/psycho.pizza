package pizza.psycho.sos.common.handler

import java.time.Instant

sealed interface ErrorMeta {
    data class Cooldown(
        val availableAt: Instant,
        val retryAfterSeconds: Long,
    ) : ErrorMeta
}

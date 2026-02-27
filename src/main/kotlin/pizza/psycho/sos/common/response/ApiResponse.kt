package pizza.psycho.sos.common.response

import org.springframework.http.HttpStatus
import java.time.Instant

sealed interface ApiResponse<out T> {
    val timestamp: Instant
    val code: String
    val message: String
}

data class Success<T>(
    override val timestamp: Instant = Instant.now(),
    override val code: String = HttpStatus.OK.name,
    override val message: String = "success",
    val data: T? = null,
) : ApiResponse<T>

fun <T> responseOf(data: T): ApiResponse<T> = Success(data = data)

fun <T> responseOf(
    message: String?,
    data: T,
): ApiResponse<T> = Success(message = message ?: "success", data = data)

fun <T> responseOf(
    status: HttpStatus,
    data: T? = null,
    message: String? = null,
): ApiResponse<T> =
    Success(
        code = status.value().toString(),
        message = message ?: status.reasonPhrase,
        data = data,
    )

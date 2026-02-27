package pizza.psycho.sos.common.response

import org.springframework.http.HttpStatus
import java.time.Instant

interface ApiResponse<out T> {
    val timestamp: Instant
    val status: Int
    val message: String
}

data class Success<T>(
    override val timestamp: Instant = Instant.now(),
    override val status: Int = HttpStatus.OK.value(),
    override val message: String = "success",
    val data: T? = null,
) : ApiResponse<T>

fun <T> responseOf(
    data: T? = null,
    status: HttpStatus = HttpStatus.OK,
    message: String = status.reasonPhrase,
): ApiResponse<T> =
    Success(
        status = status.value(),
        message = message,
        data = data,
    )

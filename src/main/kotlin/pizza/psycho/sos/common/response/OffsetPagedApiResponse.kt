package pizza.psycho.sos.common.response

import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import java.time.Instant

/**
 * Offset-based Response
 */
data class OffsetPagedApiResponse<T>(
    override val timestamp: Instant = Instant.now(),
    override val status: Int = HttpStatus.OK.value(),
    override val message: String = "success",
    val data: List<T>,
    val pageInfo: OffsetPageInfo,
) : ApiResponse<T>

data class OffsetPageInfo(
    val currentPage: Int,
    val size: Int,
    val totalPages: Int,
    val totalElements: Long,
)

fun <T> pagedResponseOf(
    page: Page<T>,
    oneIndexed: Boolean = true,
): OffsetPagedApiResponse<T> =
    OffsetPagedApiResponse(
        data = page.content,
        pageInfo = page.toOffsetPageInfo(oneIndexed),
    )

fun Page<*>.toOffsetPageInfo(oneIndexed: Boolean = true): OffsetPageInfo =
    OffsetPageInfo(
        currentPage = number + if (oneIndexed) 1 else 0,
        size = size,
        totalPages = totalPages,
        totalElements = totalElements,
    )

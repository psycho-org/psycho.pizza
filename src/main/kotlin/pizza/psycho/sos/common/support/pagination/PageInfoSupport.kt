package pizza.psycho.sos.common.support.pagination

import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import pizza.psycho.sos.common.config.PageableProperties
import pizza.psycho.sos.common.response.OffsetPageInfo
import pizza.psycho.sos.common.response.OffsetPagedApiResponse
import pizza.psycho.sos.common.response.pagedResponseOf
import pizza.psycho.sos.common.response.toOffsetPageInfo

@Component
class PageInfoSupport(
    private val props: PageableProperties,
) {
    fun <T> toPageResponse(page: Page<T>): OffsetPagedApiResponse<T> = pagedResponseOf(page, props.oneIndexed)

    fun toPageInfo(page: Page<*>): OffsetPageInfo = page.toOffsetPageInfo(props.oneIndexed)
}

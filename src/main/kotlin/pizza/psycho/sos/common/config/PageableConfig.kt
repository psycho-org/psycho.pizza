package pizza.psycho.sos.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer

@Configuration
@EnableConfigurationProperties(PageableProperties::class)
class PageableConfig(
    private val properties: PageableProperties,
) {
    @Bean
    fun pageableCustomizer(): PageableHandlerMethodArgumentResolverCustomizer =
        PageableHandlerMethodArgumentResolverCustomizer { resolver ->
            resolver.setOneIndexedParameters(properties.oneIndexed)
            resolver.setMaxPageSize(properties.maxPageSize)
        }
}

@ConfigurationProperties("common.pageable")
data class PageableProperties(
    var oneIndexed: Boolean = true,
    var maxPageSize: Int = 200,
)

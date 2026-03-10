package pizza.psycho.sos.analysis.infrastructure.llm

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient

@Configuration
class OpenAiConfig(
    @param:Value("\${openai.api-key}")
    private val apiKey: String,
) {
    @Bean
    fun openAiRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .build()
}

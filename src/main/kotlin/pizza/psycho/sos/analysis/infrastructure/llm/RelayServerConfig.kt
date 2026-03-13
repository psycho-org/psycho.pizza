package pizza.psycho.sos.analysis.infrastructure.llm

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient

@Configuration
class RelayServerConfig(
    @Value("\${app.relay-server.base-url:http://relay.psycho.pizza}")
    private val relayServerBaseUrl: String,
//    @Value("\${app.relay-server.secret:default-secret-key}")
//    private val internalSecret: String
) {
    @Bean
    fun relayServerRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl(relayServerBaseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            // TODO: secret header 검증 필요할지도?
            // .defaultHeader("X-Internal-Secret", internalSecret)
            .build()
}

package pizza.psycho.sos.identity.security.config

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
@ConfigurationProperties(prefix = "security.jwt")
class JwtProperties {
    var issuer: String = "psycho-sos"
    var accessTokenValiditySeconds: Long = 3600
    var refreshTokenValiditySeconds: Long = 1_209_600
    var refreshCookieName: String = "refresh_token"
    var refreshCookiePath: String = "/api/v1/auth"
    var refreshCookieSameSite: String = "Lax"
    var refreshCookieSecure: Boolean = false
    lateinit var secret: String

    @PostConstruct
    fun validate() {
        require(secret.toByteArray(StandardCharsets.UTF_8).size >= 32) {
            "JWT secret must be at least 32 bytes for HMAC-SHA256"
        }
    }
}

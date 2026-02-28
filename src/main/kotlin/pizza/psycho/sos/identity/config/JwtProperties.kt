package pizza.psycho.sos.identity.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

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
    var secret: String = "local-dev-secret-key-change-before-production-12345"
}

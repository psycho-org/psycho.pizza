package pizza.psycho.sos.identity.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import pizza.psycho.sos.identity.security.token.AccessTokenProvider

@Component
class JwtAuthenticationFilter(
    private val accessTokenProvider: AccessTokenProvider,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION).orEmpty()
        if (bearerToken.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            val accessToken = bearerToken.substring(BEARER_PREFIX.length).trim()
            if (accessToken.isNotBlank() && SecurityContextHolder.getContext().authentication == null) {
                val authentication = accessTokenProvider.toAuthentication(accessToken)
                if (authentication != null) {
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        }

        filterChain.doFilter(request, response)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}

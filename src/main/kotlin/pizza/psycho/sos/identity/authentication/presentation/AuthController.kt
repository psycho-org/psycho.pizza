package pizza.psycho.sos.identity.authentication.presentation

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.Success
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.authentication.application.service.AuthService
import pizza.psycho.sos.identity.authentication.application.service.dto.AuthQuery
import pizza.psycho.sos.identity.authentication.application.service.dto.AuthResult
import pizza.psycho.sos.identity.authentication.presentation.dto.AuthRequest
import pizza.psycho.sos.identity.authentication.presentation.dto.AuthResponse
import pizza.psycho.sos.identity.security.config.JwtProperties
import java.time.Duration

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtProperties: JwtProperties,
) {
    @PostMapping("/login")
    fun login(
        response: HttpServletResponse,
        @Valid @RequestBody request: AuthRequest.Login,
    ): ApiResponse<AuthResponse.Login> =
        authService
            .login(
                AuthQuery.Login(
                    email = request.email,
                    password = request.password,
                ),
            ).toLoginApiResponse(response)

    @PostMapping("/refresh")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ApiResponse<AuthResponse.Refresh> {
        val refreshToken = request.readRefreshTokenFromCookie().orEmpty()
        return authService
            .refresh(AuthQuery.Refresh(refreshToken))
            .toRefreshApiResponse(response)
    }

    @PostMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ApiResponse<AuthResponse.Logout> {
        val refreshToken = request.readRefreshTokenFromCookie().orEmpty()
        authService.logout(AuthQuery.Logout(refreshToken))
        clearRefreshCookie(response)
        return Success(
            message = "Successfully logged out",
            data = AuthResponse.Logout.Success(),
        )
    }

    private fun AuthResult.Login.toLoginApiResponse(response: HttpServletResponse): ApiResponse<AuthResponse.Login> =
        when (this) {
            is AuthResult.Login.Authenticated -> {
                writeRefreshCookie(response, refreshToken)
                responseOf(
                    data =
                        AuthResponse.Login.Authenticated(
                            accessToken = accessToken,
                            user =
                                AuthResponse.User(
                                    id = user.id,
                                    email = user.email,
                                    firstName = user.firstName,
                                    lastName = user.lastName,
                                ),
                        ),
                )
            }

            AuthResult.Login.Failure.InvalidCredentials ->
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        }

    private fun AuthResult.Refresh.toRefreshApiResponse(response: HttpServletResponse): ApiResponse<AuthResponse.Refresh> =
        when (this) {
            is AuthResult.Refresh.Authenticated -> {
                writeRefreshCookie(response, refreshToken)
                responseOf(
                    data =
                        AuthResponse.Refresh.Authenticated(
                            accessToken = accessToken,
                            user =
                                AuthResponse.User(
                                    id = user.id,
                                    email = user.email,
                                    firstName = user.firstName,
                                    lastName = user.lastName,
                                ),
                        ),
                )
            }

            AuthResult.Refresh.Failure.InvalidRefreshToken ->
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")
        }

    private fun writeRefreshCookie(
        response: HttpServletResponse,
        refreshToken: String,
    ) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            buildRefreshCookie(refreshToken, jwtProperties.refreshTokenValiditySeconds).toString(),
        )
    }

    private fun clearRefreshCookie(response: HttpServletResponse) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            buildRefreshCookie("", 0).toString(),
        )
    }

    private fun buildRefreshCookie(
        value: String,
        maxAgeSeconds: Long,
    ): ResponseCookie =
        ResponseCookie
            .from(jwtProperties.refreshCookieName, value)
            .httpOnly(true)
            .secure(jwtProperties.refreshCookieSecure)
            .path(jwtProperties.refreshCookiePath)
            .sameSite(jwtProperties.refreshCookieSameSite)
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build()

    private fun HttpServletRequest.readRefreshTokenFromCookie(): String? =
        cookies
            ?.firstOrNull { cookie: Cookie -> cookie.name == jwtProperties.refreshCookieName }
            ?.value
}

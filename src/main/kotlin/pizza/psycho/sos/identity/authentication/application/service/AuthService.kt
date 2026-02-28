package pizza.psycho.sos.identity.authentication.application.service

import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import pizza.psycho.sos.identity.authentication.application.service.dto.AuthQuery
import pizza.psycho.sos.identity.authentication.application.service.dto.AuthResult
import pizza.psycho.sos.identity.security.token.JwtTokenProvider

@Service
@Transactional
class AuthService(
    private val accountRepository: AccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenService: RefreshTokenService,
) {
    fun login(query: AuthQuery.Login): AuthResult.Login {
        val email = query.email.trim().lowercase()
        val account =
            accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                ?: return AuthResult.Login.Failure.InvalidCredentials

        if (!passwordEncoder.matches(query.password, account.passwordHash)) {
            return AuthResult.Login.Failure.InvalidCredentials
        }

        val accountId = requireNotNull(account.id) { "Account id is required for login" }
        val refreshToken = refreshTokenService.issue(accountId)
        return issueLoginToken(account, refreshToken)
    }

    fun refresh(query: AuthQuery.Refresh): AuthResult.Refresh {
        val rotated =
            refreshTokenService.rotate(query.refreshToken)
                ?: return AuthResult.Refresh.Failure.InvalidRefreshToken
        val account =
            accountRepository.findByIdAndDeletedAtIsNull(rotated.accountId)
                ?: return AuthResult.Refresh.Failure.InvalidRefreshToken

        return issueRefreshToken(account, rotated.refreshToken)
    }

    fun logout(query: AuthQuery.Logout): AuthResult.Logout {
        refreshTokenService.revoke(query.refreshToken)
        return AuthResult.Logout.Success
    }

    private fun issueLoginToken(
        account: Account,
        refreshToken: String,
    ): AuthResult.Login.Authenticated =
        AuthResult.Login.Authenticated(
            accessToken = jwtTokenProvider.issueAccessToken(account),
            refreshToken = refreshToken,
            user =
                AuthResult.User(
                    id = account.id.toString(),
                    email = account.email.orEmpty(),
                    firstName = account.givenName.orEmpty(),
                    lastName = account.familyName.orEmpty(),
                ),
        )

    private fun issueRefreshToken(
        account: Account,
        refreshToken: String,
    ): AuthResult.Refresh.Authenticated =
        AuthResult.Refresh.Authenticated(
            accessToken = jwtTokenProvider.issueAccessToken(account),
            refreshToken = refreshToken,
            user =
                AuthResult.User(
                    id = account.id.toString(),
                    email = account.email.orEmpty(),
                    firstName = account.givenName.orEmpty(),
                    lastName = account.familyName.orEmpty(),
                ),
        )
}

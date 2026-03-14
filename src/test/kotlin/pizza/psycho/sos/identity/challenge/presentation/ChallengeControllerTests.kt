package pizza.psycho.sos.identity.challenge.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.common.domain.vo.Email
import pizza.psycho.sos.identity.challenge.application.service.ChallengeService
import pizza.psycho.sos.identity.challenge.application.service.dto.ChallengeCommand
import pizza.psycho.sos.identity.challenge.application.service.dto.RequestChallengeResult
import pizza.psycho.sos.identity.challenge.application.service.dto.VerifyOtpResult
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import pizza.psycho.sos.identity.security.config.SecurityConfig
import pizza.psycho.sos.identity.security.filter.JwtAuthenticationFilter
import pizza.psycho.sos.identity.security.principal.ActiveAccountPrincipalQueryService
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.identity.security.token.AccessTokenClaims
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import java.time.Instant
import java.util.UUID

@WebMvcTest(ChallengeController::class)
@AutoConfigureMockMvc
@Import(SecurityConfig::class, JwtAuthenticationFilter::class)
@ActiveProfiles("test")
class ChallengeControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var challengeService: ChallengeService

    @MockitoBean
    private lateinit var accessTokenProvider: AccessTokenProvider

    @MockitoBean
    private lateinit var activeAccountPrincipalQueryService: ActiveAccountPrincipalQueryService

    @Test
    fun `register request endpoint returns challenge id`() {
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000101")
        val expiresAt = Instant.parse("2026-03-15T00:05:00Z")
        `when`(
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "user@psycho.pizza",
                    operationType = OperationType.REGISTER,
                ),
            ),
        ).thenReturn(RequestChallengeResult.Success(challengeId, expiresAt))

        mockMvc
            .perform(
                post("/api/v1/accounts/register/requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"user@psycho.pizza"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.challengeId").value(challengeId.toString()))
            .andExpect(jsonPath("$.data.expiresAt").value(expiresAt.toString()))
    }

    @Test
    fun `register request endpoint maps cooldown to too many requests`() {
        val availableAt = Instant.parse("2026-03-15T00:00:43Z")
        `when`(
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "user@psycho.pizza",
                    operationType = OperationType.REGISTER,
                ),
            ),
        ).thenReturn(RequestChallengeResult.Failure.CooldownActive(availableAt, 43))

        mockMvc
            .perform(
                post("/api/v1/accounts/register/requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"user@psycho.pizza"}"""),
            ).andExpect(status().isTooManyRequests)
            .andExpect(header().string(HttpHeaders.RETRY_AFTER, "43"))
            .andExpect(jsonPath("$.code").value("CHALLENGE_OTP_COOLDOWN_ACTIVE"))
            .andExpect(jsonPath("$.meta.availableAt").value(availableAt.toString()))
            .andExpect(jsonPath("$.meta.retryAfterSeconds").value(43))
    }

    @Test
    fun `register confirmation returns token and verified email`() {
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000102")
        val tokenId = UUID.fromString("00000000-0000-0000-0000-000000000103")
        `when`(
            challengeService.verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = challengeId,
                    otpCode = "123456",
                    expectedOperationType = OperationType.REGISTER,
                    requesterEmail = null,
                ),
            ),
        ).thenReturn(
            VerifyOtpResult.Success(
                confirmationTokenId = tokenId,
                targetEmail = Email.of("user@psycho.pizza"),
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/accounts/register/confirmations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"challengeId":"$challengeId","otpCode":"123456"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.confirmationTokenId").value(tokenId.toString()))
            .andExpect(jsonPath("$.data.verifiedEmail").value("user@psycho.pizza"))
    }

    @Test
    fun `register confirmation maps invalid otp to unauthorized`() {
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000104")
        `when`(
            challengeService.verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = challengeId,
                    otpCode = "000000",
                    expectedOperationType = OperationType.REGISTER,
                    requesterEmail = null,
                ),
            ),
        ).thenReturn(VerifyOtpResult.Failure.InvalidOtp)

        mockMvc
            .perform(
                post("/api/v1/accounts/register/confirmations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"challengeId":"$challengeId","otpCode":"000000"}"""),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("CHALLENGE_INVALID_OTP"))
    }

    @Test
    fun `register confirmation maps operation type mismatch to not found`() {
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000104")
        `when`(
            challengeService.verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = challengeId,
                    otpCode = "000000",
                    expectedOperationType = OperationType.REGISTER,
                    requesterEmail = null,
                ),
            ),
        ).thenReturn(VerifyOtpResult.Failure.OperationTypeMismatch)

        mockMvc
            .perform(
                post("/api/v1/accounts/register/confirmations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"challengeId":"$challengeId","otpCode":"000000"}"""),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `me password request endpoint requires authentication`() {
        mockMvc
            .perform(
                post("/api/v1/accounts/me/password/requests")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `me password request endpoint uses principal email when authenticated`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000105"),
                email = "me@psycho.pizza",
            )
        val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000106")
        val expiresAt = Instant.parse("2026-03-15T00:06:00Z")
        `when`(
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "me@psycho.pizza",
                    operationType = OperationType.CHANGE_PASSWORD,
                ),
            ),
        ).thenReturn(RequestChallengeResult.Success(challengeId, expiresAt))

        mockMvc
            .perform(
                post("/api/v1/accounts/me/password/requests")
                    .with(authentication(authentication))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.challengeId").value(challengeId.toString()))
            .andExpect(jsonPath("$.data.expiresAt").value(expiresAt.toString()))
    }

    @Test
    fun `me password request endpoint uses database-backed principal email instead of jwt claim`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000107"),
                email = "db@psycho.pizza",
            )
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000108")
        val expiresAt = Instant.parse("2026-03-15T00:07:00Z")
        `when`(accessTokenProvider.parse("access-token")).thenReturn(
            AccessTokenClaims(
                accountId = principal.accountId,
                email = "tampered@psycho.pizza",
            ),
        )
        `when`(activeAccountPrincipalQueryService.findActivePrincipalByAccountId(principal.accountId)).thenReturn(principal)
        `when`(
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "db@psycho.pizza",
                    operationType = OperationType.CHANGE_PASSWORD,
                ),
            ),
        ).thenReturn(RequestChallengeResult.Success(challengeId, expiresAt))

        mockMvc
            .perform(
                post("/api/v1/accounts/me/password/requests")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.challengeId").value(challengeId.toString()))
            .andExpect(jsonPath("$.data.expiresAt").value(expiresAt.toString()))
    }

    @Test
    fun `me withdraw request endpoint returns challenge id and expiresAt`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000120"),
                email = "me@psycho.pizza",
            )
        val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000121")
        val expiresAt = Instant.parse("2026-03-15T00:08:00Z")
        `when`(
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "me@psycho.pizza",
                    operationType = OperationType.WITHDRAW,
                ),
            ),
        ).thenReturn(RequestChallengeResult.Success(challengeId, expiresAt))

        mockMvc
            .perform(
                post("/api/v1/accounts/me/withdraw/requests")
                    .with(authentication(authentication))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.challengeId").value(challengeId.toString()))
            .andExpect(jsonPath("$.data.expiresAt").value(expiresAt.toString()))
    }

    @Test
    fun `me withdraw confirmation uses principal email and withdraw operation type`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000107"),
                email = "me@psycho.pizza",
            )
        val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000108")
        val tokenId = UUID.fromString("00000000-0000-0000-0000-000000000109")
        `when`(
            challengeService.verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = challengeId,
                    otpCode = "123456",
                    expectedOperationType = OperationType.WITHDRAW,
                    requesterEmail = "me@psycho.pizza",
                ),
            ),
        ).thenReturn(
            VerifyOtpResult.Success(
                confirmationTokenId = tokenId,
                targetEmail = Email.of("me@psycho.pizza"),
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/accounts/me/withdraw/confirmations")
                    .with(authentication(authentication))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"challengeId":"$challengeId","otpCode":"123456"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.confirmationTokenId").value(tokenId.toString()))
            .andExpect(jsonPath("$.data.verifiedEmail").value("me@psycho.pizza"))
    }

    @Test
    fun `me password confirmation maps requester mismatch to not found`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000110"),
                email = "me@psycho.pizza",
            )
        val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000111")
        `when`(
            challengeService.verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = challengeId,
                    otpCode = "123456",
                    expectedOperationType = OperationType.CHANGE_PASSWORD,
                    requesterEmail = "me@psycho.pizza",
                ),
            ),
        ).thenReturn(VerifyOtpResult.Failure.RequesterEmailMismatch)

        mockMvc
            .perform(
                post("/api/v1/accounts/me/password/confirmations")
                    .with(authentication(authentication))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"challengeId":"$challengeId","otpCode":"123456"}"""),
            ).andExpect(status().isNotFound)
    }
}

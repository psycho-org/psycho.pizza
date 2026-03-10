package pizza.psycho.sos.identity.challenge.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.identity.challenge.application.service.ChallengeService
import pizza.psycho.sos.identity.challenge.application.service.dto.ChallengeCommand
import pizza.psycho.sos.identity.challenge.application.service.dto.RequestChallengeResult
import pizza.psycho.sos.identity.challenge.application.service.dto.VerifyOtpResult
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import pizza.psycho.sos.identity.security.config.SecurityConfig
import pizza.psycho.sos.identity.security.filter.JwtAuthenticationFilter
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
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

    @Test
    fun `register request endpoint returns challenge id`() {
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000101")
        `when`(
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "user@psycho.pizza",
                    operationType = OperationType.REGISTER,
                ),
            ),
        ).thenReturn(RequestChallengeResult.Success(challengeId))

        mockMvc
            .perform(
                post("/api/v1/accounts/register/requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"user@psycho.pizza"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.challengeId").value(challengeId.toString()))
    }

    @Test
    fun `register request endpoint maps cooldown to too many requests`() {
        `when`(
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "user@psycho.pizza",
                    operationType = OperationType.REGISTER,
                ),
            ),
        ).thenReturn(RequestChallengeResult.Failure.CooldownActive)

        mockMvc
            .perform(
                post("/api/v1/accounts/register/requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"user@psycho.pizza"}"""),
            ).andExpect(status().isTooManyRequests)
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
                ),
            ),
        ).thenReturn(
            VerifyOtpResult.Success(
                confirmationTokenId = tokenId,
                targetEmail = "user@psycho.pizza",
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
                ),
            ),
        ).thenReturn(VerifyOtpResult.Failure.InvalidOtp)

        mockMvc
            .perform(
                post("/api/v1/accounts/register/confirmations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"challengeId":"$challengeId","otpCode":"000000"}"""),
            ).andExpect(status().isUnauthorized)
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
        `when`(
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "me@psycho.pizza",
                    operationType = OperationType.CHANGE_PASSWORD,
                ),
            ),
        ).thenReturn(RequestChallengeResult.Success(challengeId))

        mockMvc
            .perform(
                post("/api/v1/accounts/me/password/requests")
                    .with(authentication(authentication))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.challengeId").value(challengeId.toString()))
    }
}

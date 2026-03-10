package pizza.psycho.sos.identity.challenge.presentation

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.challenge.application.service.ChallengeService
import pizza.psycho.sos.identity.challenge.application.service.dto.ChallengeCommand
import pizza.psycho.sos.identity.challenge.application.service.dto.RequestChallengeResult
import pizza.psycho.sos.identity.challenge.application.service.dto.VerifyOtpResult
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import pizza.psycho.sos.identity.challenge.presentation.dto.ChallengeRequest
import pizza.psycho.sos.identity.challenge.presentation.dto.ChallengeResponse
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal

@RestController
@RequestMapping("/api/v1/accounts")
class ChallengeController(
    private val challengeService: ChallengeService,
) {
    @PostMapping("/register/requests")
    fun requestRegister(
        @Valid @RequestBody request: ChallengeRequest.RequestOtp,
    ): ApiResponse<ChallengeResponse.Requested> =
        challengeService
            .createChallenge(
                ChallengeCommand.Request(
                    email = request.email,
                    operationType = OperationType.REGISTER,
                ),
            ).toRequestedResponse()

    @PostMapping("/register/confirmations")
    fun confirmRegister(
        @Valid @RequestBody request: ChallengeRequest.VerifyOtp,
    ): ApiResponse<ChallengeResponse.Confirmed> =
        challengeService
            .verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = request.challengeId,
                    otpCode = request.otpCode,
                ),
            ).toConfirmedResponse()

    @PostMapping("/me/withdraw/requests")
    fun requestWithdraw(
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<ChallengeResponse.Requested> =
        challengeService
            .createChallenge(
                ChallengeCommand.Request(
                    email = principal.email,
                    operationType = OperationType.WITHDRAW,
                ),
            ).toRequestedResponse()

    @PostMapping("/me/withdraw/confirmations")
    fun confirmWithdraw(
        @Valid @RequestBody request: ChallengeRequest.VerifyOtp,
    ): ApiResponse<ChallengeResponse.Confirmed> =
        challengeService
            .verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = request.challengeId,
                    otpCode = request.otpCode,
                ),
            ).toConfirmedResponse()

    @PostMapping("/me/password/requests")
    fun requestPasswordUpdate(
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<ChallengeResponse.Requested> =
        challengeService
            .createChallenge(
                ChallengeCommand.Request(
                    email = principal.email,
                    operationType = OperationType.CHANGE_PASSWORD,
                ),
            ).toRequestedResponse()

    @PostMapping("/me/password/confirmations")
    fun confirmPasswordUpdate(
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
        @Valid @RequestBody request: ChallengeRequest.VerifyOtp,
    ): ApiResponse<ChallengeResponse.Confirmed> =
        challengeService
            .verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = request.challengeId,
                    otpCode = request.otpCode,
                ),
            ).toConfirmedResponse()

    private fun RequestChallengeResult.toRequestedResponse(): ApiResponse<ChallengeResponse.Requested> =
        when (this) {
            is RequestChallengeResult.Success ->
                responseOf(
                    data = ChallengeResponse.Requested(challengeId = challengeId),
                )

            RequestChallengeResult.Failure.CooldownActive ->
                throw ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait before requesting a new OTP",
                )
        }

    private fun VerifyOtpResult.toConfirmedResponse(): ApiResponse<ChallengeResponse.Confirmed> =
        when (this) {
            is VerifyOtpResult.Success ->
                responseOf(
                    data =
                        ChallengeResponse.Confirmed(
                            confirmationTokenId = confirmationTokenId,
                            verifiedEmail = targetEmail.value,
                        ),
                )

            VerifyOtpResult.Failure.ChallengeNotFound ->
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found")

            VerifyOtpResult.Failure.ChallengeExpired ->
                throw ResponseStatusException(HttpStatus.GONE, "Challenge has expired")

            VerifyOtpResult.Failure.MaxAttemptsExceeded ->
                throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Maximum attempts exceeded")

            VerifyOtpResult.Failure.InvalidOtp ->
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP code")
        }
}

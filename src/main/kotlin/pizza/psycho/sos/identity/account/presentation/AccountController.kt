package pizza.psycho.sos.identity.account.presentation

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.application.service.dto.AccountResult
import pizza.psycho.sos.identity.account.presentation.dto.RegisterRequest
import pizza.psycho.sos.identity.account.presentation.dto.RegisterResponse

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountService: AccountService,
) {
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
    ): ApiResponse<RegisterResponse> =
        accountService
            .register(
                AccountCommand.Register(
                    email = request.email,
                    password = request.password,
                    firstName = request.firstName,
                    lastName = request.lastName,
                ),
            ).toApiResponse()

    private fun AccountResult.toApiResponse(): ApiResponse<RegisterResponse> =
        when (this) {
            is AccountResult.Registered ->
                responseOf(
                    data =
                        RegisterResponse(
                            id = account.id,
                            email = account.email,
                            firstName = account.firstName,
                            lastName = account.lastName,
                        ),
                )

            AccountResult.Failure.EmailAlreadyRegistered ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "Email already registered")
        }
}

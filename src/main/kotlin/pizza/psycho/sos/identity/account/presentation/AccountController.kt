package pizza.psycho.sos.identity.account.presentation

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PatchMapping
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
import pizza.psycho.sos.identity.account.presentation.dto.AccountRequest
import pizza.psycho.sos.identity.account.presentation.dto.AccountResponse
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountService: AccountService,
) {
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: AccountRequest.Register,
    ): ApiResponse<AccountResponse.Register> =
        accountService
            .register(
                AccountCommand.Register(
                    email = request.email,
                    password = request.password,
                    firstName = request.firstName,
                    lastName = request.lastName,
                ),
            ).toRegisterApiResponse()

    @PatchMapping("/me/display-name")
    fun updateDisplayName(
        authentication: Authentication,
        @Valid @RequestBody request: AccountRequest.UpdateDisplayName,
    ): ApiResponse<AccountResponse.UpdateDisplayName> {
        val principal =
            authentication.principal as? AuthenticatedAccountPrincipal
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")

        return accountService
            .updateDisplayName(
                AccountCommand.UpdateDisplayName(
                    accountId = principal.accountId,
                    displayName = request.displayName,
                ),
            ).toUpdateDisplayNameApiResponse()
    }

    private fun AccountResult.toRegisterApiResponse(): ApiResponse<AccountResponse.Register> =
        when (this) {
            is AccountResult.Registered ->
                responseOf(
                    data =
                        AccountResponse.Register(
                            id = account.id,
                            email = account.email,
                            firstName = account.firstName,
                            lastName = account.lastName,
                        ),
                )

            AccountResult.Failure.EmailAlreadyRegistered -> throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email already registered",
            )

            else -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }

    private fun AccountResult.toUpdateDisplayNameApiResponse(): ApiResponse<AccountResponse.UpdateDisplayName> =
        when (this) {
            is AccountResult.Updated.DisplayName ->
                responseOf(
                    data =
                        AccountResponse.UpdateDisplayName(
                            displayName = displayName,
                        ),
                )

            AccountResult.Failure.InvalidDisplayName -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid display name",
            )

            AccountResult.Failure.AccountNotFound -> throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found",
            )

            else -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }
}

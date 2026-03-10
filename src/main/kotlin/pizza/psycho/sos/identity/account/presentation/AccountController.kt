package pizza.psycho.sos.identity.account.presentation

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
import pizza.psycho.sos.identity.account.presentation.dto.AccountRequest
import pizza.psycho.sos.identity.account.presentation.dto.AccountResponse
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.identity.account.application.service.dto.RegisterAccountResult as Register
import pizza.psycho.sos.identity.account.application.service.dto.UpdateAccountResult as Update
import pizza.psycho.sos.identity.account.application.service.dto.WithdrawAccountResult as Withdraw

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountService: AccountService,
) {
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: AccountRequest.Register,
    ): ApiResponse<AccountResponse.Registered> =
        accountService
            .register(
                AccountCommand.Register(
                    email = request.email,
                    password = request.password,
                    firstName = request.givenName,
                    lastName = request.familyName,
                ),
            ).toApiResponse()

    @PatchMapping("/me/update/display-name")
    fun updateDisplayName(
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
        @Valid @RequestBody request: AccountRequest.Update.DisplayName,
    ): ApiResponse<AccountResponse.Updated> =
        accountService
            .updateDisplayName(
                AccountCommand.Update.DisplayName(
                    accountId = principal.accountId,
                    displayName = request.displayName,
                ),
            ).toApiResponse()

    @PostMapping("/me/withdraw")
    fun withdraw(
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
        @Valid @RequestBody request: AccountRequest.Withdraw,
    ): ApiResponse<AccountResponse.Withdrawn> =
        accountService
            .withdraw(
                AccountCommand.Withdraw(
                    accountId = principal.accountId,
                    password = request.password,
                ),
            ).toApiResponse()

    private fun Register.toApiResponse(): ApiResponse<AccountResponse.Registered> =
        when (this) {
            is Register.Success ->
                responseOf(
                    data =
                        AccountResponse.Registered(
                            email = email,
                            displayName = displayName,
                        ),
                )

            Register.Failure.EmailAlreadyRegistered -> throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email already registered",
            )
        }

    private fun Update.toApiResponse(): ApiResponse<AccountResponse.Updated> =
        when (this) {
            is Update.Success.DisplayName ->
                responseOf(
                    data =
                        AccountResponse.Updated.DisplayName(
                            displayName = displayName,
                        ),
                )

            Update.Success.Password ->
                responseOf(AccountResponse.Updated.UpdatedPassword)

            Update.Success.Name ->
                responseOf(AccountResponse.Updated.Name)

            Update.Failure.AccountNotFound -> throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found",
            )

            Update.Failure.InvalidDisplayName -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid display name",
            )
        }

    private fun Withdraw.toApiResponse(): ApiResponse<AccountResponse.Withdrawn> =
        when (this) {
            is Withdraw.Success ->
                responseOf(AccountResponse.Withdrawn)

            Withdraw.Failure.AccountNotFound -> throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found",
            )

            Withdraw.Failure.InvalidCredentials -> throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials",
            )

            Withdraw.Failure.OwnerWorkspaceExists -> throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Transfer ownership or delete owned workspaces before withdrawing",
            )
        }
}

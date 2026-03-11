package pizza.psycho.sos.identity.account.presentation

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
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.presentation.dto.AccountRequest
import pizza.psycho.sos.identity.account.presentation.dto.AccountResponse
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.identity.account.application.service.dto.RegisterAccountResult as Register
import pizza.psycho.sos.identity.account.application.service.dto.UpdateNameAccountResult as Name
import pizza.psycho.sos.identity.account.application.service.dto.UpdatePasswordAccountResult as Password
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
                    confirmationTokenId = request.confirmationTokenId,
                    password = request.password,
                    firstName = request.givenName,
                    lastName = request.familyName,
                ),
            ).toApiResponse()

    @PostMapping("/me/update/name")
    fun updateName(
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
        @Valid @RequestBody request: AccountRequest.Update.Name,
    ): ApiResponse<AccountResponse.Updated> =
        accountService
            .updateName(
                AccountCommand.Update.Name(
                    accountId = principal.accountId,
                    givenName = request.givenName,
                    familyName = request.familyName,
                ),
            ).toApiResponse()

    @PostMapping("/me/password")
    fun updatePassword(
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
        @Valid @RequestBody request: AccountRequest.Update.Password,
    ): ApiResponse<AccountResponse.Updated.Password> =
        accountService
            .updatePassword(
                AccountCommand.Update.Password(
                    accountId = principal.accountId,
                    confirmationTokenId = request.confirmationTokenId,
                    currentPassword = request.currentPassword,
                    newPassword = request.newPassword,
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
                    confirmationTokenId = request.confirmationTokenId,
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
                            givenName = givenName,
                            familyName = familyName,
                        ),
                )

            Register.Failure.EmailAlreadyRegistered -> throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email already registered",
            )

            Register.Failure.InvalidConfirmationToken -> throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid or expired confirmation token",
            )

            Register.Failure.InvalidName -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid name",
            )
        }

    private fun Name.toApiResponse(): ApiResponse<AccountResponse.Updated.Name> {
        when (this) {
            is Name.Success -> return responseOf(AccountResponse.Updated.Name)

            Name.Failure.AccountNotFound -> throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found",
            )

            Name.Failure.InvalidName -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid name",
            )
        }
    }

    private fun Password.toApiResponse(): ApiResponse<AccountResponse.Updated.Password> {
        when (this) {
            is Password.Success -> return responseOf(AccountResponse.Updated.Password)

            Password.Failure.AccountNotFound -> throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found",
            )

            Password.Failure.InvalidCredentials -> throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials",
            )

            Password.Failure.InvalidConfirmationToken -> throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid or expired confirmation token",
            )
        }
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

            Withdraw.Failure.InvalidConfirmationToken -> throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid or expired confirmation token",
            )
        }
}

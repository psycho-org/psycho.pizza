package pizza.psycho.sos.identity.account.presentation

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.domain.exception.AccountErrorCode
import pizza.psycho.sos.identity.account.presentation.dto.AccountRequest
import pizza.psycho.sos.identity.account.presentation.dto.AccountResponse
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.identity.account.application.service.dto.RegisterAccountResult as Register
import pizza.psycho.sos.identity.account.application.service.dto.UpdateDisplayNameAccountResult as DisplayName
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
                            displayName = displayName,
                        ),
                )

            Register.Failure.EmailAlreadyRegistered -> throw DomainException(AccountErrorCode.ACCOUNT_EMAIL_ALREADY_REGISTERED)
            Register.Failure.InvalidConfirmationToken -> throw DomainException(AccountErrorCode.ACCOUNT_INVALID_CONFIRMATION_TOKEN)
        }

    private fun DisplayName.toApiResponse(): ApiResponse<AccountResponse.Updated.DisplayName> =
        when (this) {
            is DisplayName.Success ->
                responseOf(
                    data =
                        AccountResponse.Updated.DisplayName(
                            displayName = displayName,
                        ),
                )

            DisplayName.Failure.AccountNotFound -> throw DomainException(AccountErrorCode.ACCOUNT_NOT_FOUND)
            DisplayName.Failure.InvalidDisplayName -> throw DomainException(AccountErrorCode.ACCOUNT_INVALID_DISPLAY_NAME)
        }

    private fun Name.toApiResponse(): ApiResponse<AccountResponse.Updated.Name> {
        when (this) {
            is Name.Success -> return responseOf(AccountResponse.Updated.Name)

            Name.Failure.AccountNotFound -> throw DomainException(AccountErrorCode.ACCOUNT_NOT_FOUND)
            Name.Failure.InvalidDisplayName -> throw DomainException(AccountErrorCode.ACCOUNT_INVALID_DISPLAY_NAME)
        }
    }

    private fun Password.toApiResponse(): ApiResponse<AccountResponse.Updated.Password> {
        when (this) {
            is Password.Success -> return responseOf(AccountResponse.Updated.Password)

            Password.Failure.AccountNotFound -> throw DomainException(AccountErrorCode.ACCOUNT_NOT_FOUND)
            Password.Failure.InvalidCredentials -> throw DomainException(AccountErrorCode.ACCOUNT_INVALID_CREDENTIALS)
            Password.Failure.InvalidConfirmationToken -> throw DomainException(AccountErrorCode.ACCOUNT_INVALID_CONFIRMATION_TOKEN)
        }
    }

    private fun Withdraw.toApiResponse(): ApiResponse<AccountResponse.Withdrawn> =
        when (this) {
            is Withdraw.Success ->
                responseOf(AccountResponse.Withdrawn)

            Withdraw.Failure.AccountNotFound -> throw DomainException(AccountErrorCode.ACCOUNT_NOT_FOUND)
            Withdraw.Failure.InvalidCredentials -> throw DomainException(AccountErrorCode.ACCOUNT_INVALID_CREDENTIALS)
            Withdraw.Failure.OwnerWorkspaceExists ->
                throw DomainException(AccountErrorCode.ACCOUNT_WITHDRAWAL_BLOCKED_BY_OWNED_WORKSPACE)
            Withdraw.Failure.InvalidConfirmationToken -> throw DomainException(AccountErrorCode.ACCOUNT_INVALID_CONFIRMATION_TOKEN)
        }
}

package pizza.psycho.sos.identity.account.application.service.dto

sealed interface RegisterAccountResult {
    data class Success(
        val email: String,
        val givenName: String,
        val familyName: String,
    ) : RegisterAccountResult

    sealed interface Failure : RegisterAccountResult {
        data object EmailAlreadyRegistered : Failure

        data object InvalidConfirmationToken : Failure

        data object InvalidName : Failure
    }
}

sealed interface UpdateNameAccountResult {
    data class Success(
        val givenName: String,
        val familyName: String,
    ) : UpdateNameAccountResult

    sealed interface Failure : UpdateNameAccountResult {
        data object AccountNotFound : Failure

        data object InvalidName : Failure
    }
}

sealed interface UpdatePasswordAccountResult {
    data object Success : UpdatePasswordAccountResult

    sealed interface Failure : UpdatePasswordAccountResult {
        data object AccountNotFound : Failure

        data object InvalidCredentials : Failure

        data object InvalidConfirmationToken : Failure
    }
}

sealed interface WithdrawAccountResult {
    data object Success : WithdrawAccountResult

    sealed interface Failure : WithdrawAccountResult {
        data object AccountNotFound : Failure

        data object InvalidCredentials : Failure

        data object OwnerWorkspaceExists : Failure

        data object InvalidConfirmationToken : Failure
    }
}

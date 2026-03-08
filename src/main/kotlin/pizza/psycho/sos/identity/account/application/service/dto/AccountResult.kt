package pizza.psycho.sos.identity.account.application.service.dto

sealed interface RegisterAccountResult {
    data class Success(
        val email: String,
        val displayName: String,
    ) : RegisterAccountResult

    sealed interface Failure : RegisterAccountResult {
        data object EmailAlreadyRegistered : Failure
    }
}

sealed interface UpdateAccountResult {
    sealed interface Success : UpdateAccountResult {
        data class DisplayName(
            val displayName: String,
        ) : Success

        data object Password : Success

        data object Name : Success
    }

    sealed interface Failure : UpdateAccountResult {
        data object AccountNotFound : Failure

        data object InvalidDisplayName : Failure
    }
}

sealed interface WithdrawAccountResult {
    data object Success : WithdrawAccountResult

    sealed interface Failure : WithdrawAccountResult {
        data object AccountNotFound : Failure

        data object InvalidCredentials : Failure

        data object OwnerWorkspaceExists : Failure
    }
}

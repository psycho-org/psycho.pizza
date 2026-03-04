package pizza.psycho.sos.identity.account.application.service.dto

sealed interface AccountResult {
    data class Registered(
        val account: AccountSnapshot,
    ) : AccountResult

    sealed interface Updated : AccountResult {
        data class DisplayName(
            val displayName: String,
        ) : Updated
    }

    sealed interface Failure : AccountResult {
        data object EmailAlreadyRegistered : Failure

        data object InvalidDisplayName : Failure

        data object AccountNotFound : Failure
    }
}

data class AccountSnapshot(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
)

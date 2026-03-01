package pizza.psycho.sos.identity.account.application.service.dto

sealed interface AccountResult {
    data class Registered(
        val account: AccountSnapshot,
    ) : AccountResult

    sealed interface Failure : AccountResult {
        data object EmailAlreadyRegistered : Failure
    }
}

data class AccountSnapshot(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
)

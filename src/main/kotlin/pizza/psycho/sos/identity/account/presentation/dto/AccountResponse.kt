package pizza.psycho.sos.identity.account.presentation.dto

sealed interface AccountResponse {
    data class Register(
        val id: String,
        val email: String,
        val firstName: String,
        val lastName: String,
    )

    data class Withdraw(
        // TODO
        val example: Nothing,
    )
}

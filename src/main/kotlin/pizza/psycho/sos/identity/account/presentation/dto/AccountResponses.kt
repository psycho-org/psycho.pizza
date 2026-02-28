package pizza.psycho.sos.identity.account.presentation.dto

data class RegisterResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
)

data class WithdrawResponse(
    // TODO
    val example: Nothing,
)

package pizza.psycho.sos.identity.account.application.service.dto

sealed interface AccountCommand {
    data class Register(
        val email: String,
        val password: String,
        val firstName: String,
        val lastName: String,
    ) : AccountCommand
}

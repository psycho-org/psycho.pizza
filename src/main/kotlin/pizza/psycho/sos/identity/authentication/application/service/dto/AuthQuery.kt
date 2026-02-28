package pizza.psycho.sos.identity.authentication.application.service.dto

sealed interface AuthQuery {
    data class Login(
        val email: String,
        val password: String,
    ) : AuthQuery

    data class Refresh(
        val refreshToken: String,
    ) : AuthQuery

    data class Logout(
        val refreshToken: String,
    ) : AuthQuery
}

package pizza.psycho.sos.identity.authentication.presentation.dto

sealed interface AuthResponse {
    sealed interface Login : AuthResponse {
        data class Authenticated(
            val accessToken: String,
            val user: User,
        ) : Login
    }

    sealed interface Refresh : AuthResponse {
        data class Authenticated(
            val accessToken: String,
            val user: User,
        ) : Refresh
    }

    sealed interface Logout : AuthResponse {
        data class Success(
            val success: Boolean = true,
        ) : Logout
    }

    data class User(
        val id: String,
        val email: String,
        val firstName: String,
        val lastName: String,
    )
}

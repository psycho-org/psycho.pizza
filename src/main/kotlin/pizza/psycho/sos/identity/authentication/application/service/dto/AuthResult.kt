package pizza.psycho.sos.identity.authentication.application.service.dto

sealed interface AuthResult {
    sealed interface Login : AuthResult {
        data class Authenticated(
            val accessToken: String,
            val refreshToken: String,
            val user: User,
        ) : Login

        sealed interface Failure : Login {
            data object InvalidCredentials : Failure
        }
    }

    sealed interface Refresh : AuthResult {
        data class Authenticated(
            val accessToken: String,
            val refreshToken: String,
            val user: User,
        ) : Refresh

        sealed interface Failure : Refresh {
            data object InvalidRefreshToken : Failure
        }
    }

    sealed interface Logout : AuthResult {
        data object Success : Logout
    }

    data class User(
        val id: String,
        val email: String,
        val firstName: String,
        val lastName: String,
    )
}

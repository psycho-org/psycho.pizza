package pizza.psycho.sos.identity.account.presentation.dto

sealed interface AccountResponse {
    data class Registered(
        val email: String,
        val givenName: String,
        val familyName: String,
    ) : AccountResponse

    sealed interface Updated {
        data object Name : Updated

        data object Password : Updated
    }

    data object Withdrawn : AccountResponse

    sealed interface Policy : AccountResponse {
        data class Password(
            val regex: String,
            val message: String,
        ) : Policy
    }
}

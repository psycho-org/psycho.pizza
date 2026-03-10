package pizza.psycho.sos.identity.account.presentation.dto

sealed interface AccountResponse {
    data class Registered(
        val email: String,
        val displayName: String,
    ) : AccountResponse

    sealed interface Updated {
        data class DisplayName(
            val displayName: String,
        ) : Updated

        data object Name : Updated

        data object UpdatedPassword : Updated
    }

    data object Withdrawn : AccountResponse
}

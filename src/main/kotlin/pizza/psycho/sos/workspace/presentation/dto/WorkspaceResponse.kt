package pizza.psycho.sos.workspace.presentation.dto

sealed interface WorkspaceResponse {
    data class Detail(
        val id: String,
        val name: String,
        val description: String,
    )

    data class Deleted(
        val id: String,
    )

    data class Member(
        val accountId: String,
        val role: String,
    )
}

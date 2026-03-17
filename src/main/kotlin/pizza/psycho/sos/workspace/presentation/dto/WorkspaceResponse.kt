package pizza.psycho.sos.workspace.presentation.dto

sealed interface WorkspaceResponse {
    data class ListItem(
        val id: String,
        val title: String,
        val role: String,
    )

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

    data class MemberListItem(
        val membershipId: String,
        val accountId: String,
        val name: String,
        val role: String,
        val joinedAt: String?,
    )
}

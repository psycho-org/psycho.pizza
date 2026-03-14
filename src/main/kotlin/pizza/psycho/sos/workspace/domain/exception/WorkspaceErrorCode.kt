package pizza.psycho.sos.workspace.domain.exception

import org.springframework.http.HttpStatus
import pizza.psycho.sos.common.exception.BaseErrorCode

enum class WorkspaceErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Workspace not found"),
    WORKSPACE_MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "Workspace membership not found"),
    WORKSPACE_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "Only workspace owner can perform this action"),
    WORKSPACE_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "Workspace membership already exists"),
    WORKSPACE_OWNER_REMOVAL_FORBIDDEN(HttpStatus.BAD_REQUEST, "Workspace owner cannot be removed"),
    WORKSPACE_TRANSFER_OWNERSHIP_FAILED(HttpStatus.BAD_REQUEST, "Failed to transfer workspace ownership"),
    WORKSPACE_ADD_MEMBER_FAILED(HttpStatus.BAD_REQUEST, "Failed to add workspace member"),
    WORKSPACE_REMOVE_MEMBER_FAILED(HttpStatus.BAD_REQUEST, "Failed to remove workspace member"),
    ;

    override val code: String = name
}

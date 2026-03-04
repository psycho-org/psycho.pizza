package pizza.psycho.sos.workspace.presentation

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.workspace.application.service.WorkspaceService
import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import pizza.psycho.sos.workspace.presentation.dto.WorkspaceRequest
import pizza.psycho.sos.workspace.presentation.dto.WorkspaceResponse
import java.util.UUID

@RestController
@RequestMapping("/api/v1/workspaces")
class WorkspaceController(
    private val workspaceService: WorkspaceService,
) {
    @PostMapping
    fun create(
        @Valid @RequestBody request: WorkspaceRequest.Create,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<WorkspaceResponse.Detail> {
        val workspace =
            workspaceService.createWorkspace(
                name = request.name,
                description = request.description,
                ownerAccountId = principal.accountId,
            )
        return responseOf(data = workspace.toDetail())
    }

    @GetMapping("/{workspaceId}")
    fun get(
        @PathVariable workspaceId: UUID,
    ): ApiResponse<WorkspaceResponse.Detail> {
        val workspace = workspaceService.getWorkspace(workspaceId)
        return responseOf(data = workspace.toDetail())
    }

    @PostMapping("/{workspaceId}/transfer-owner")
    fun transferOwner(
        @PathVariable workspaceId: UUID,
        @Valid @RequestBody request: WorkspaceRequest.TransferOwner,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<WorkspaceResponse.Detail> {
        val workspace =
            workspaceService.transferOwnership(
                workspaceId = workspaceId,
                requesterAccountId = principal.accountId,
                newOwnerAccountId = request.newOwnerAccountId,
            )
        return responseOf(data = workspace.toDetail())
    }

    @DeleteMapping("/{workspaceId}")
    fun delete(
        @PathVariable workspaceId: UUID,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<WorkspaceResponse.Deleted> {
        workspaceService.deleteWorkspace(workspaceId, principal.accountId)
        return responseOf(
            data =
                WorkspaceResponse.Deleted(
                    id = workspaceId.toString(),
                ),
        )
    }

    @PostMapping("/{workspaceId}/members")
    fun addMember(
        @PathVariable workspaceId: UUID,
        @Valid @RequestBody request: WorkspaceRequest.AddMember,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<WorkspaceResponse.Member> {
        val membership =
            workspaceService.addMember(
                workspaceId = workspaceId,
                requesterAccountId = principal.accountId,
                accountId = request.accountId,
                role = request.role,
            )
        return responseOf(
            data =
                WorkspaceResponse.Member(
                    accountId = membership.accountId.toString(),
                    role = membership.role.name,
                ),
        )
    }

    @DeleteMapping("/{workspaceId}/members")
    fun removeMember(
        @PathVariable workspaceId: UUID,
        @Valid @RequestBody request: WorkspaceRequest.RemoveMember,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<WorkspaceResponse.Member> {
        workspaceService.removeMember(
            workspaceId = workspaceId,
            requesterAccountId = principal.accountId,
            targetAccountId = request.accountId,
        )
        return responseOf(
            data =
                WorkspaceResponse.Member(
                    accountId = request.accountId.toString(),
                    role = "REMOVED",
                ),
        )
    }

    private fun Workspace.toDetail(): WorkspaceResponse.Detail =
        WorkspaceResponse.Detail(
            id = id.toString(),
            name = name,
            description = description,
        )
}

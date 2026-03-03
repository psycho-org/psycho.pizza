package pizza.psycho.sos.workspace.presentation

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
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
    ): ApiResponse<WorkspaceResponse.Detail> {
        val workspace =
            workspaceService.createWorkspace(
                name = request.name,
                description = request.description,
                ownerAccountId = request.ownerAccountId,
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
    ): ApiResponse<WorkspaceResponse.Detail> {
        val workspace =
            workspaceService.transferOwnership(
                workspaceId = workspaceId,
                requesterAccountId = request.requesterAccountId,
                newOwnerAccountId = request.newOwnerAccountId,
            )
        return responseOf(data = workspace.toDetail())
    }

    @DeleteMapping("/{workspaceId}")
    fun delete(
        @PathVariable workspaceId: UUID,
        @Valid @RequestBody request: WorkspaceRequest.Delete,
    ): ApiResponse<WorkspaceResponse.Deleted> {
        workspaceService.deleteWorkspace(workspaceId, request.requesterAccountId)
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
    ): ApiResponse<WorkspaceResponse.Member> {
        val membership =
            workspaceService.addMember(
                workspaceId = workspaceId,
                requesterAccountId = request.requesterAccountId,
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
    ): ApiResponse<WorkspaceResponse.Member> {
        workspaceService.removeMember(
            workspaceId = workspaceId,
            requesterAccountId = request.requesterAccountId,
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

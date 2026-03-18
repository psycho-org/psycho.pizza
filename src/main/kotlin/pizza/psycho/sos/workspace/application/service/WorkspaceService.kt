package pizza.psycho.sos.workspace.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.workspace.application.dto.ActiveWorkspaceMembership
import pizza.psycho.sos.workspace.application.dto.WorkspaceMemberListItem
import pizza.psycho.sos.workspace.application.port.out.AccountDisplayNamePort
import pizza.psycho.sos.workspace.domain.exception.WorkspaceErrorCode
import pizza.psycho.sos.workspace.domain.model.membership.Membership
import pizza.psycho.sos.workspace.domain.model.membership.Role
import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import pizza.psycho.sos.workspace.domain.repository.WorkspaceCommandRepository
import pizza.psycho.sos.workspace.domain.repository.WorkspaceMembershipQueryRepository
import pizza.psycho.sos.workspace.domain.repository.WorkspaceQueryRepository
import java.util.UUID

@Service
class WorkspaceService(
    private val workspaceCommandRepository: WorkspaceCommandRepository,
    private val workspaceQueryRepository: WorkspaceQueryRepository,
    private val workspaceMembershipQueryRepository: WorkspaceMembershipQueryRepository,
    private val accountDisplayNamePort: AccountDisplayNamePort,
) {
    @Transactional
    fun createWorkspace(
        name: String,
        description: String,
        ownerAccountId: UUID,
    ): Workspace {
        logger.info("Creating workspace. ownerAccountId={} name={}", ownerAccountId, name)
        val workspace = Workspace.create(name, description, ownerAccountId, resolveDisplayName(ownerAccountId))
        val saved = workspaceCommandRepository.save(workspace)
        logger.info("Workspace created. workspaceId={}", saved.id)
        return saved
    }

    @Transactional(readOnly = true)
    fun getWorkspace(workspaceId: UUID): Workspace {
        logger.info("Fetching workspace. workspaceId={}", workspaceId)
        return workspaceQueryRepository.findActiveByIdOrNull(workspaceId)
            ?: run {
                logger.warn("Workspace not found. workspaceId={}", workspaceId)
                throw DomainException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND)
            }
    }

    @Transactional(readOnly = true)
    fun findActiveWorkspaceMembershipsByAccountId(accountId: UUID): List<ActiveWorkspaceMembership> {
        logger.info("Fetching active workspace memberships. accountId={}", accountId)
        return workspaceMembershipQueryRepository.findActiveWorkspaceMembershipsByAccountId(accountId)
    }

    @Transactional(readOnly = true)
    fun listMembers(
        workspaceId: UUID,
        requesterAccountId: UUID,
    ): List<WorkspaceMemberListItem> {
        logger.info("Listing workspace members. workspaceId={} requesterAccountId={}", workspaceId, requesterAccountId)
        requireActiveWorkspace(workspaceId)
        workspaceMembershipQueryRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, requesterAccountId)
            ?: run {
                logger.warn(
                    "Membership not found for list members. workspaceId={} requesterAccountId={}",
                    workspaceId,
                    requesterAccountId,
                )
                throw DomainException(WorkspaceErrorCode.WORKSPACE_MEMBERSHIP_NOT_FOUND)
            }
        return workspaceMembershipQueryRepository.findActiveMembersByWorkspaceId(workspaceId)
    }

    @Transactional
    fun transferOwnership(
        workspaceId: UUID,
        requesterAccountId: UUID,
        newOwnerAccountId: UUID,
    ): Workspace {
        logger.info(
            "Transferring ownership. workspaceId={} requesterAccountId={} newOwnerAccountId={}",
            workspaceId,
            requesterAccountId,
            newOwnerAccountId,
        )
        val workspace = requireActiveWorkspace(workspaceId)
        val requesterRole =
            workspaceMembershipQueryRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, requesterAccountId)
                ?: run {
                    logger.warn(
                        "Membership not found for transfer ownership. workspaceId={} requesterAccountId={}",
                        workspaceId,
                        requesterAccountId,
                    )
                    throw DomainException(WorkspaceErrorCode.WORKSPACE_MEMBERSHIP_NOT_FOUND)
                }
        if (!requesterRole.isOwner()) {
            logger.warn(
                "Requester is not owner for transfer ownership. workspaceId={} requesterAccountId={}",
                workspaceId,
                requesterAccountId,
            )
            throw DomainException(WorkspaceErrorCode.WORKSPACE_OWNER_REQUIRED)
        }
        try {
            workspace.transferOwnership(requesterAccountId, newOwnerAccountId)
        } catch (ex: IllegalArgumentException) {
            logger.warn(
                "Failed to transfer ownership. workspaceId={} requesterAccountId={} newOwnerAccountId={} reason={}",
                workspaceId,
                requesterAccountId,
                newOwnerAccountId,
                ex.message,
            )
            val errorCode =
                if (ex.message?.startsWith("membership not found") == true) {
                    WorkspaceErrorCode.WORKSPACE_MEMBERSHIP_NOT_FOUND
                } else {
                    WorkspaceErrorCode.WORKSPACE_TRANSFER_OWNERSHIP_FAILED
                }
            throw DomainException(errorCode, ex.message ?: errorCode.message, ex)
        }
        logger.info("Ownership transferred. workspaceId={}", workspaceId)
        return workspace
    }

    @Transactional
    fun deleteWorkspace(
        workspaceId: UUID,
        requesterAccountId: UUID,
    ) {
        logger.info("Deleting workspace. workspaceId={} requesterAccountId={}", workspaceId, requesterAccountId)
        val workspace = requireActiveWorkspace(workspaceId)
        val requesterRole =
            workspaceMembershipQueryRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, requesterAccountId)
                ?: run {
                    logger.warn(
                        "Membership not found for delete. workspaceId={} requesterAccountId={}",
                        workspaceId,
                        requesterAccountId,
                    )
                    throw DomainException(WorkspaceErrorCode.WORKSPACE_MEMBERSHIP_NOT_FOUND)
                }

        if (!requesterRole.isOwner()) {
            logger.warn(
                "Requester is not owner for delete. workspaceId={} requesterAccountId={}",
                workspaceId,
                requesterAccountId,
            )
            throw DomainException(WorkspaceErrorCode.WORKSPACE_OWNER_REQUIRED)
        }

        workspace.removeAllMemberships(requesterAccountId)
        workspace.delete(requesterAccountId)
        logger.info("Workspace deleted. workspaceId={}", workspaceId)
    }

    @Transactional
    fun addMember(
        workspaceId: UUID,
        requesterAccountId: UUID,
        accountId: UUID,
        role: Role = Role.CREW,
    ): Membership {
        logger.info(
            "Adding workspace member. workspaceId={} requesterAccountId={} accountId={} role={}",
            workspaceId,
            requesterAccountId,
            accountId,
            role,
        )
        val workspace = requireActiveWorkspace(workspaceId)
        val requesterRole =
            workspaceMembershipQueryRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, requesterAccountId)
                ?: run {
                    logger.warn(
                        "Membership not found for add member. workspaceId={} requesterAccountId={}",
                        workspaceId,
                        requesterAccountId,
                    )
                    throw DomainException(WorkspaceErrorCode.WORKSPACE_MEMBERSHIP_NOT_FOUND)
                }
        if (!requesterRole.isOwner()) {
            logger.warn(
                "Requester is not owner for add member. workspaceId={} requesterAccountId={}",
                workspaceId,
                requesterAccountId,
            )
            throw DomainException(WorkspaceErrorCode.WORKSPACE_OWNER_REQUIRED)
        }

        return try {
            workspace.addMembership(accountId, resolveDisplayName(accountId), role)
        } catch (ex: IllegalArgumentException) {
            logger.warn(
                "Failed to add member. workspaceId={} requesterAccountId={} accountId={} role={} reason={}",
                workspaceId,
                requesterAccountId,
                accountId,
                role,
                ex.message,
            )
            val errorCode =
                if (ex.message?.startsWith("membership already exists") == true) {
                    WorkspaceErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS
                } else {
                    WorkspaceErrorCode.WORKSPACE_ADD_MEMBER_FAILED
                }
            throw DomainException(errorCode, ex.message ?: errorCode.message, ex)
        }
    }

    @Transactional
    fun removeMember(
        workspaceId: UUID,
        requesterAccountId: UUID,
        targetAccountId: UUID,
    ) {
        logger.info(
            "Removing workspace member. workspaceId={} requesterAccountId={} targetAccountId={}",
            workspaceId,
            requesterAccountId,
            targetAccountId,
        )
        val workspace = requireActiveWorkspace(workspaceId)
        val requesterRole =
            workspaceMembershipQueryRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, requesterAccountId)
                ?: run {
                    logger.warn(
                        "Membership not found for remove member. workspaceId={} requesterAccountId={}",
                        workspaceId,
                        requesterAccountId,
                    )
                    throw DomainException(WorkspaceErrorCode.WORKSPACE_MEMBERSHIP_NOT_FOUND)
                }
        if (!requesterRole.isOwner()) {
            logger.warn(
                "Requester is not owner for remove member. workspaceId={} requesterAccountId={}",
                workspaceId,
                requesterAccountId,
            )
            throw DomainException(WorkspaceErrorCode.WORKSPACE_OWNER_REQUIRED)
        }

        val targetMembership =
            workspace.memberships.firstOrNull { it.accountId == targetAccountId && !it.isDeleted }
                ?: run {
                    logger.warn(
                        "Target membership not found for remove member. workspaceId={} targetAccountId={}",
                        workspaceId,
                        targetAccountId,
                    )
                    throw DomainException(WorkspaceErrorCode.WORKSPACE_MEMBERSHIP_NOT_FOUND)
                }
        if (targetMembership.role.isOwner()) {
            logger.warn(
                "Attempt to remove owner member. workspaceId={} targetAccountId={}",
                workspaceId,
                targetAccountId,
            )
            throw DomainException(WorkspaceErrorCode.WORKSPACE_OWNER_REMOVAL_FORBIDDEN)
        }

        try {
            workspace.removeMembership(targetAccountId, requesterAccountId)
        } catch (ex: IllegalArgumentException) {
            logger.warn(
                "Failed to remove member. workspaceId={} requesterAccountId={} targetAccountId={} reason={}",
                workspaceId,
                requesterAccountId,
                targetAccountId,
                ex.message,
            )
            val errorCode =
                if (ex.message?.startsWith("membership not found") == true) {
                    WorkspaceErrorCode.WORKSPACE_MEMBERSHIP_NOT_FOUND
                } else {
                    WorkspaceErrorCode.WORKSPACE_REMOVE_MEMBER_FAILED
                }
            throw DomainException(errorCode, ex.message ?: errorCode.message, ex)
        }
    }

    private fun requireActiveWorkspace(workspaceId: UUID): Workspace =
        workspaceQueryRepository.findActiveByIdOrNull(workspaceId)
            ?: run {
                logger.warn("Workspace not found. workspaceId={}", workspaceId)
                throw DomainException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND)
            }

    private fun resolveDisplayName(accountId: UUID): String = accountDisplayNamePort.findActiveDisplayNameByAccountId(accountId)

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
    }
}

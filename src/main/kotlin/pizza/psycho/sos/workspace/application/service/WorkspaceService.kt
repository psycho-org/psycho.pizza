package pizza.psycho.sos.workspace.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.workspace.domain.model.membership.Membership
import pizza.psycho.sos.workspace.domain.model.membership.Role
import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import pizza.psycho.sos.workspace.domain.repository.MembershipRepository
import pizza.psycho.sos.workspace.domain.repository.WorkspaceRepository
import java.util.UUID

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val membershipRepository: MembershipRepository,
) {
    @Transactional
    fun createWorkspace(
        name: String,
        description: String,
        ownerAccountId: UUID,
    ): Workspace {
        logger.info("Creating workspace. ownerAccountId={} name={}", ownerAccountId, name)
        val workspace = Workspace.create(name, description, ownerAccountId)
        val saved = workspaceRepository.save(workspace)
        logger.info("Workspace created. workspaceId={}", saved.id)
        return saved
    }

    @Transactional(readOnly = true)
    fun getWorkspace(workspaceId: UUID): Workspace {
        logger.info("Fetching workspace. workspaceId={}", workspaceId)
        return workspaceRepository.findActiveByIdOrNull(workspaceId)
            ?: run {
                logger.warn("Workspace not found. workspaceId={}", workspaceId)
                throw DomainException("workspace not found. workspaceId=$workspaceId")
            }
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
            membershipRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, requesterAccountId)
                ?: run {
                    logger.warn(
                        "Membership not found for transfer ownership. workspaceId={} requesterAccountId={}",
                        workspaceId,
                        requesterAccountId,
                    )
                    throw DomainException("membership not found for requesterAccountId=$requesterAccountId")
                }
        if (!requesterRole.isOwner()) {
            logger.warn(
                "Requester is not owner for transfer ownership. workspaceId={} requesterAccountId={}",
                workspaceId,
                requesterAccountId,
            )
            throw DomainException("only owner can transfer ownership")
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
            throw DomainException(ex.message ?: "failed to transfer ownership", ex)
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
            membershipRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, requesterAccountId)
                ?: run {
                    logger.warn(
                        "Membership not found for delete. workspaceId={} requesterAccountId={}",
                        workspaceId,
                        requesterAccountId,
                    )
                    throw DomainException("membership not found for requesterAccountId=$requesterAccountId")
                }

        if (!requesterRole.isOwner()) {
            logger.warn(
                "Requester is not owner for delete. workspaceId={} requesterAccountId={}",
                workspaceId,
                requesterAccountId,
            )
            throw DomainException("only owner can delete workspace")
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
            membershipRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, requesterAccountId)
                ?: run {
                    logger.warn(
                        "Membership not found for add member. workspaceId={} requesterAccountId={}",
                        workspaceId,
                        requesterAccountId,
                    )
                    throw DomainException("membership not found for requesterAccountId=$requesterAccountId")
                }
        if (!requesterRole.isOwner()) {
            logger.warn(
                "Requester is not owner for add member. workspaceId={} requesterAccountId={}",
                workspaceId,
                requesterAccountId,
            )
            throw DomainException("only owner can add member")
        }

        return try {
            workspace.addMembership(accountId, role)
        } catch (ex: IllegalArgumentException) {
            logger.warn(
                "Failed to add member. workspaceId={} requesterAccountId={} accountId={} role={} reason={}",
                workspaceId,
                requesterAccountId,
                accountId,
                role,
                ex.message,
            )
            throw DomainException(ex.message ?: "failed to add member", ex)
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
            membershipRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, requesterAccountId)
                ?: run {
                    logger.warn(
                        "Membership not found for remove member. workspaceId={} requesterAccountId={}",
                        workspaceId,
                        requesterAccountId,
                    )
                    throw DomainException("membership not found for requesterAccountId=$requesterAccountId")
                }
        if (!requesterRole.isOwner()) {
            logger.warn(
                "Requester is not owner for remove member. workspaceId={} requesterAccountId={}",
                workspaceId,
                requesterAccountId,
            )
            throw DomainException("only owner can remove member")
        }

        val targetMembership =
            workspace.memberships.firstOrNull { it.accountId == targetAccountId && !it.isDeleted }
                ?: run {
                    logger.warn(
                        "Target membership not found for remove member. workspaceId={} targetAccountId={}",
                        workspaceId,
                        targetAccountId,
                    )
                    throw DomainException("membership not found for accountId=$targetAccountId")
                }
        if (targetMembership.role.isOwner()) {
            logger.warn(
                "Attempt to remove owner member. workspaceId={} targetAccountId={}",
                workspaceId,
                targetAccountId,
            )
            throw DomainException("cannot remove owner")
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
            throw DomainException(ex.message ?: "failed to remove member", ex)
        }
    }

    private fun requireActiveWorkspace(workspaceId: UUID): Workspace =
        workspaceRepository.findActiveByIdOrNull(workspaceId)
            ?: run {
                logger.warn("Workspace not found. workspaceId={}", workspaceId)
                throw DomainException("workspace not found. workspaceId=$workspaceId")
            }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
    }
}

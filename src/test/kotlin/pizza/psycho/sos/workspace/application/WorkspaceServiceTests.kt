package pizza.psycho.sos.workspace.application

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.workspace.application.dto.ActiveWorkspaceMembership
import pizza.psycho.sos.workspace.application.dto.WorkspaceMemberListItem
import pizza.psycho.sos.workspace.application.port.out.AccountDisplayNamePort
import pizza.psycho.sos.workspace.application.service.WorkspaceService
import pizza.psycho.sos.workspace.domain.exception.WorkspaceErrorCode
import pizza.psycho.sos.workspace.domain.model.membership.Role
import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import pizza.psycho.sos.workspace.domain.repository.WorkspaceCommandRepository
import pizza.psycho.sos.workspace.domain.repository.WorkspaceMembershipQueryRepository
import pizza.psycho.sos.workspace.domain.repository.WorkspaceQueryRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
class WorkspaceServiceTests {
    private val workspaceQueryRepository = FakeWorkspaceQueryRepository()
    private val workspaceCommandRepository = FakeWorkspaceCommandRepository(workspaceQueryRepository)
    private val workspaceMembershipQueryRepository =
        org.mockito.Mockito.mock(WorkspaceMembershipQueryRepository::class.java)
    private val accountDisplayNamePort = org.mockito.Mockito.mock(AccountDisplayNamePort::class.java)
    private val service =
        WorkspaceService(
            workspaceCommandRepository,
            workspaceQueryRepository,
            workspaceMembershipQueryRepository,
            accountDisplayNamePort,
        )

    @Test
    fun `createWorkspace - creates and saves workspace`() {
        val ownerAccountId = UUID.randomUUID()
        Mockito
            .`when`(accountDisplayNamePort.findActiveDisplayNameByAccountIdOrNull(ownerAccountId))
            .thenReturn("Owner Name")

        val workspace = service.createWorkspace("Test", "Desc", ownerAccountId)

        assertEquals("Test", workspace.name)
        assertEquals("Desc", workspace.description)
        assertEquals(1, workspace.memberships.size)
        assertEquals("Owner Name", workspace.memberships.first().displayName)
        assertNotNull(workspaceCommandRepository.lastSaved)
    }

    @Test
    fun `getWorkspace - throws when workspace missing`() {
        val workspaceId = UUID.randomUUID()

        val exception =
            assertFailsWith<DomainException> {
                service.getWorkspace(workspaceId)
            }

        assertEquals(WorkspaceErrorCode.WORKSPACE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `findActiveWorkspaceMembershipsByAccountId delegates to membership repository`() {
        val accountId = UUID.randomUUID()
        val memberships =
            listOf(
                ActiveWorkspaceMembership(UUID.randomUUID(), "Alpha", Role.OWNER),
                ActiveWorkspaceMembership(UUID.randomUUID(), "Beta", Role.CREW),
            )
        Mockito
            .`when`(workspaceMembershipQueryRepository.findActiveWorkspaceMembershipsByAccountId(accountId))
            .thenReturn(memberships)

        val result = service.findActiveWorkspaceMembershipsByAccountId(accountId)

        assertEquals(memberships, result)
    }

    @Test
    fun `listMembers returns active members when requester belongs to workspace`() {
        val workspaceId = UUID.randomUUID()
        val requesterAccountId = UUID.randomUUID()
        val member =
            WorkspaceMemberListItem(
                membershipId = UUID.randomUUID(),
                accountId = UUID.randomUUID(),
                name = "Crew Member",
                role = Role.CREW,
                joinedAt = Instant.parse("2026-03-14T11:26:28Z"),
            )
        val workspace = Workspace.create("Alpha", "Desc", requesterAccountId)
        workspace.id = workspaceId
        workspaceCommandRepository.save(workspace)
        Mockito
            .`when`(
                workspaceMembershipQueryRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, requesterAccountId),
            ).thenReturn(Role.OWNER)
        Mockito
            .`when`(workspaceMembershipQueryRepository.findActiveMembersByWorkspaceId(workspaceId))
            .thenReturn(listOf(member))

        val result = service.listMembers(workspaceId, requesterAccountId)

        assertEquals(listOf(member), result)
        Mockito.verify(workspaceMembershipQueryRepository).findActiveMembersByWorkspaceId(workspaceId)
    }

    @Test
    fun `addMember stores account display name on membership`() {
        val workspaceId = UUID.randomUUID()
        val ownerAccountId = UUID.randomUUID()
        val crewAccountId = UUID.randomUUID()
        val workspace = Workspace.create("Test", "Desc", ownerAccountId, "Owner Name")
        workspace.id = workspaceId
        workspaceCommandRepository.save(workspace)
        Mockito
            .`when`(
                workspaceMembershipQueryRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, ownerAccountId),
            ).thenReturn(Role.OWNER)
        Mockito
            .`when`(accountDisplayNamePort.findActiveDisplayNameByAccountIdOrNull(crewAccountId))
            .thenReturn("Crew Member")

        val membership = service.addMember(workspaceId, ownerAccountId, crewAccountId, Role.CREW)

        assertEquals("Crew Member", membership.displayName)
    }

    @Test
    fun `transferOwnership - updates membership roles`() {
        val workspaceId = UUID.randomUUID()
        val ownerAccountId = UUID.randomUUID()
        val crewAccountId = UUID.randomUUID()
        val workspace = Workspace.create("Test", "Desc", ownerAccountId)
        workspace.addMembership(crewAccountId, role = Role.CREW)
        workspace.id = workspaceId
        workspaceCommandRepository.save(workspace)

        Mockito
            .`when`(
                workspaceMembershipQueryRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, ownerAccountId),
            ).thenReturn(Role.OWNER)
        service.transferOwnership(workspaceId, ownerAccountId, crewAccountId)

        val ownerMembership = workspace.memberships.first { it.accountId == ownerAccountId }
        val crewMembership = workspace.memberships.first { it.accountId == crewAccountId }
        assertEquals(Role.CREW, ownerMembership.role)
        assertEquals(Role.OWNER, crewMembership.role)
    }

    @Test
    fun `deleteWorkspace - soft deletes memberships`() {
        val workspaceId = UUID.randomUUID()
        val ownerAccountId = UUID.randomUUID()
        val crewAccountId = UUID.randomUUID()
        val workspace = Workspace.create("Test", "Desc", ownerAccountId)
        workspace.addMembership(crewAccountId, role = Role.CREW)
        workspace.id = workspaceId
        workspaceCommandRepository.save(workspace)

        Mockito
            .`when`(
                workspaceMembershipQueryRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, ownerAccountId),
            ).thenReturn(Role.OWNER)
        service.deleteWorkspace(workspaceId, ownerAccountId)

        assertTrue(workspace.memberships.all { it.isDeleted })
        assertNotNull(workspace.deletedAt)
        assertEquals(ownerAccountId, workspace.deletedBy)
    }

    private class FakeWorkspaceQueryRepository : WorkspaceQueryRepository {
        private val store = mutableMapOf<UUID, Workspace>()

        override fun findByIdOrNull(id: UUID): Workspace? = store[id]

        override fun findActiveByIdOrNull(id: UUID): Workspace? = store[id]?.takeIf { !it.isDeleted }

        fun saveInternal(workspace: Workspace): Workspace {
            if (workspace.id == null) {
                workspace.id = UUID.randomUUID()
            }
            store[workspace.id!!] = workspace
            return workspace
        }
    }

    private class FakeWorkspaceCommandRepository(
        private val workspaceQueryRepository: FakeWorkspaceQueryRepository,
    ) : WorkspaceCommandRepository {
        var lastSaved: Workspace? = null
            private set

        override fun save(workspace: Workspace): Workspace {
            workspaceQueryRepository.saveInternal(workspace)
            lastSaved = workspace
            return workspace
        }
    }
}

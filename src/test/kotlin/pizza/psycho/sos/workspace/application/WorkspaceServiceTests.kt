package pizza.psycho.sos.workspace.application

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.workspace.application.service.WorkspaceService
import pizza.psycho.sos.workspace.domain.model.membership.Role
import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import pizza.psycho.sos.workspace.domain.repository.MembershipRepository
import pizza.psycho.sos.workspace.domain.repository.WorkspaceRepository
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
class WorkspaceServiceTests {
    private val workspaceRepository = FakeWorkspaceRepository()
    private val membershipRepository = org.mockito.Mockito.mock(MembershipRepository::class.java)
    private val service = WorkspaceService(workspaceRepository, membershipRepository)

    @Test
    fun `createWorkspace - creates and saves workspace`() {
        val workspace = service.createWorkspace("Test", "Desc", UUID.randomUUID())

        assertEquals("Test", workspace.name)
        assertEquals("Desc", workspace.description)
        assertEquals(1, workspace.memberships.size)
        assertNotNull(workspaceRepository.lastSaved)
    }

    @Test
    fun `getWorkspace - throws when workspace missing`() {
        val workspaceId = UUID.randomUUID()

        assertFailsWith<DomainException> {
            service.getWorkspace(workspaceId)
        }
    }

    @Test
    fun `transferOwnership - updates membership roles`() {
        val workspaceId = UUID.randomUUID()
        val ownerAccountId = UUID.randomUUID()
        val crewAccountId = UUID.randomUUID()
        val workspace = Workspace.create("Test", "Desc", ownerAccountId)
        workspace.addMembership(crewAccountId, Role.CREW)
        workspace.id = workspaceId
        workspaceRepository.save(workspace)

        `when`(membershipRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, ownerAccountId)).thenReturn(Role.OWNER)
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
        workspace.addMembership(crewAccountId, Role.CREW)
        workspace.id = workspaceId
        workspaceRepository.save(workspace)

        `when`(membershipRepository.findRoleByWorkspaceIdAndAccountId(workspaceId, ownerAccountId)).thenReturn(Role.OWNER)
        service.deleteWorkspace(workspaceId, ownerAccountId)

        assertTrue(workspace.memberships.all { it.isDeleted })
        assertNotNull(workspace.deletedAt)
        assertEquals(ownerAccountId, workspace.deletedBy)
    }

    @Test
    fun `existsActiveOwnerMembershipByAccountId - delegates to membership repository`() {
        val accountId = UUID.randomUUID()
        `when`(membershipRepository.existsActiveOwnerMembershipByAccountId(accountId)).thenReturn(true)

        val result = service.existsActiveOwnerMembershipByAccountId(accountId)

        assertTrue(result)
    }

    private class FakeWorkspaceRepository : WorkspaceRepository {
        private val store = mutableMapOf<UUID, Workspace>()
        var lastSaved: Workspace? = null
            private set

        override fun findByIdOrNull(id: UUID): Workspace? = store[id]

        override fun findActiveByIdOrNull(id: UUID): Workspace? = store[id]?.takeIf { !it.isDeleted }

        override fun save(workspace: Workspace): Workspace {
            if (workspace.id == null) {
                workspace.id = UUID.randomUUID()
            }
            store[workspace.id!!] = workspace
            lastSaved = workspace
            return workspace
        }
    }
}

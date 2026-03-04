package pizza.psycho.sos.workspace.domain.model.workspace

import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.workspace.domain.model.membership.Role
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@ActiveProfiles("test")
class WorkspaceTests {
    @Test
    fun `create - owner membership is created with owner role`() {
        val ownerAccountId = UUID.randomUUID()

        val workspace = Workspace.create("Test", "Desc", ownerAccountId)

        assertEquals("Test", workspace.name)
        assertEquals("Desc", workspace.description)
        assertEquals(1, workspace.memberships.size)

        val membership = workspace.memberships.first()
        assertEquals(ownerAccountId, membership.accountId)
        assertEquals(Role.OWNER, membership.role)
    }

    @Test
    fun `addMembership - duplicate membership throws`() {
        val ownerAccountId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val workspace = Workspace.create("Test", "Desc", ownerAccountId)

        workspace.addMembership(accountId)

        assertFailsWith<IllegalArgumentException> {
            workspace.addMembership(accountId)
        }
    }

    @Test
    fun `transferOwnership - only owner can transfer`() {
        val ownerAccountId = UUID.randomUUID()
        val crewAccountId = UUID.randomUUID()
        val workspace = Workspace.create("Test", "Desc", ownerAccountId)
        workspace.addMembership(crewAccountId, Role.CREW)

        assertFailsWith<IllegalArgumentException> {
            workspace.transferOwnership(crewAccountId, ownerAccountId)
        }
    }

    @Test
    fun `transferOwnership - owner role is moved`() {
        val ownerAccountId = UUID.randomUUID()
        val crewAccountId = UUID.randomUUID()
        val workspace = Workspace.create("Test", "Desc", ownerAccountId)
        workspace.addMembership(crewAccountId, Role.CREW)

        workspace.transferOwnership(ownerAccountId, crewAccountId)

        val ownerMembership = workspace.memberships.first { it.accountId == ownerAccountId }
        val crewMembership = workspace.memberships.first { it.accountId == crewAccountId }
        assertEquals(Role.CREW, ownerMembership.role)
        assertEquals(Role.OWNER, crewMembership.role)
    }

    @Test
    fun `removeAllMemberships - soft deletes memberships`() {
        val ownerAccountId = UUID.randomUUID()
        val crewAccountId = UUID.randomUUID()
        val workspace = Workspace.create("Test", "Desc", ownerAccountId)
        workspace.addMembership(crewAccountId, Role.CREW)

        workspace.removeAllMemberships(ownerAccountId)

        assertTrue(workspace.memberships.all { it.isDeleted })
    }
}

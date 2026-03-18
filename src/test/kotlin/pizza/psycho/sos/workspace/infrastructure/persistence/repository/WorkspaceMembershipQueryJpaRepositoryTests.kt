package pizza.psycho.sos.workspace.infrastructure.persistence.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.workspace.domain.model.membership.Role
import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import java.util.UUID

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class WorkspaceMembershipQueryJpaRepositoryTests {
    @Autowired
    private lateinit var workspaceMembershipQueryJpaRepository: WorkspaceMembershipQueryJpaRepository

    @Autowired
    private lateinit var workspaceCommandJpaRepository: WorkspaceCommandJpaRepository

    @Test
    fun `existsActiveMembershipByWorkspaceIdAndAccountId returns true when membership exists`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000921")
        val savedWorkspace = workspaceCommandJpaRepository.save(Workspace.create("Omicron", "desc", accountId))

        val result =
            workspaceMembershipQueryJpaRepository.existsActiveMembershipByWorkspaceIdAndAccountId(savedWorkspace.id!!, accountId)

        assertTrue(result)
    }

    @Test
    fun `existsActiveMembershipByWorkspaceIdAndAccountId returns false when membership is deleted`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000923")
        val workspace =
            Workspace.create("Pi", "desc", accountId).also {
                it.memberships.first().delete(UUID.fromString("00000000-0000-0000-0000-000000000924"))
            }
        val savedWorkspace = workspaceCommandJpaRepository.save(workspace)

        val result =
            workspaceMembershipQueryJpaRepository.existsActiveMembershipByWorkspaceIdAndAccountId(savedWorkspace.id!!, accountId)

        assertFalse(result)
    }

    @Test
    fun `existsActiveMembershipByWorkspaceIdAndAccountId returns false when workspace is deleted`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000925")
        val workspace =
            Workspace.create("Rho", "desc", accountId).also {
                it.delete(UUID.fromString("00000000-0000-0000-0000-000000000926"))
            }
        val savedWorkspace = workspaceCommandJpaRepository.save(workspace)

        val result =
            workspaceMembershipQueryJpaRepository.existsActiveMembershipByWorkspaceIdAndAccountId(savedWorkspace.id!!, accountId)

        assertFalse(result)
    }

    @Test
    fun `findActiveWorkspaceMembershipsByAccountId returns active memberships`() {
        val ownerAccountId = UUID.fromString("00000000-0000-0000-0000-000000000908")
        val crewWorkspace =
            Workspace.create("Zeta", "desc", UUID.fromString("00000000-0000-0000-0000-000000000903")).also {
                it.addMembership(ownerAccountId, role = Role.CREW)
            }

        workspaceCommandJpaRepository.save(Workspace.create("Epsilon", "desc", ownerAccountId))
        workspaceCommandJpaRepository.save(crewWorkspace)

        val memberships = workspaceMembershipQueryJpaRepository.findActiveWorkspaceMembershipsByAccountId(ownerAccountId)

        assertEquals(2, memberships.size)
        assertEquals(setOf("Epsilon", "Zeta"), memberships.map { it.workspaceTitle }.toSet())
        assertEquals(1, memberships.count { it.role == Role.OWNER })
        assertEquals(1, memberships.count { it.role == Role.CREW })
    }

    @Test
    fun `findActiveWorkspaceMembershipsByAccountId excludes deleted membership and deleted workspace`() {
        val ownerAccountId = UUID.fromString("00000000-0000-0000-0000-000000000909")

        val deletedMembershipWorkspace = Workspace.create("Eta", "desc", ownerAccountId)
        deletedMembershipWorkspace.memberships.first().delete(UUID.fromString("00000000-0000-0000-0000-000000000910"))
        workspaceCommandJpaRepository.save(deletedMembershipWorkspace)

        val deletedWorkspace =
            Workspace.create("Theta", "desc", ownerAccountId).also {
                it.delete(UUID.fromString("00000000-0000-0000-0000-000000000911"))
            }
        workspaceCommandJpaRepository.save(deletedWorkspace)

        val activeWorkspace = Workspace.create("Iota", "desc", ownerAccountId)
        workspaceCommandJpaRepository.save(activeWorkspace)

        val memberships = workspaceMembershipQueryJpaRepository.findActiveWorkspaceMembershipsByAccountId(ownerAccountId)

        assertEquals(1, memberships.size)
        assertEquals("Iota", memberships.first().workspaceTitle)
        assertEquals(Role.OWNER, memberships.first().role)
    }

    @Test
    fun `existsActiveOwnerMembershipByAccountId returns true only for active owner membership`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000912")
        workspaceCommandJpaRepository.save(Workspace.create("Kappa", "desc", accountId))
        workspaceCommandJpaRepository.save(
            Workspace.create("Lambda", "desc", UUID.fromString("00000000-0000-0000-0000-000000000913")).also {
                it.addMembership(accountId, role = Role.CREW)
            },
        )

        val result = workspaceMembershipQueryJpaRepository.existsActiveOwnerMembershipByAccountId(accountId)

        assertEquals(true, result)
    }

    @Test
    fun `existsActiveOwnerMembershipByAccountId returns false when only non-owner memberships exist`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000916")
        workspaceCommandJpaRepository.save(
            Workspace.create("Nu", "desc", UUID.fromString("00000000-0000-0000-0000-000000000917")).also {
                it.addMembership(accountId, role = Role.CREW)
            },
        )

        val result = workspaceMembershipQueryJpaRepository.existsActiveOwnerMembershipByAccountId(accountId)

        assertFalse(result)
    }

    @Test
    fun `findActiveMembersByWorkspaceId returns membership display name without account join`() {
        val ownerAccountId = UUID.fromString("00000000-0000-0000-0000-000000000914")
        val crewAccountId = UUID.fromString("00000000-0000-0000-0000-000000000915")
        val workspace =
            Workspace.create("Mu", "desc", ownerAccountId).also {
                it.addMembership(crewAccountId, role = Role.CREW).displayName = "Crew Display"
            }
        workspace.memberships.first { it.accountId == ownerAccountId }.displayName = "Owner Display"
        val savedWorkspace = workspaceCommandJpaRepository.save(workspace)

        val members = workspaceMembershipQueryJpaRepository.findActiveMembersByWorkspaceId(savedWorkspace.id!!)

        assertEquals(2, members.size)
        assertEquals(setOf("Owner Display", "Crew Display"), members.map { it.name }.toSet())
        assertEquals(setOf(Role.OWNER, Role.CREW), members.map { it.role }.toSet())
    }
}

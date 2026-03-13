package pizza.psycho.sos.workspace.infrastructure.persistence.repository

import org.junit.jupiter.api.Assertions.assertEquals
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
    private lateinit var workspaceJpaRepository: WorkspaceJpaRepository

    @Test
    fun `findActiveWorkspaceMembershipsByAccountId returns active memberships`() {
        val ownerAccountId = UUID.fromString("00000000-0000-0000-0000-000000000908")
        val crewWorkspace =
            Workspace.create("Zeta", "desc", UUID.fromString("00000000-0000-0000-0000-000000000903")).also {
                it.addMembership(ownerAccountId, Role.CREW)
            }

        workspaceJpaRepository.save(Workspace.create("Epsilon", "desc", ownerAccountId))
        workspaceJpaRepository.save(crewWorkspace)

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
        workspaceJpaRepository.save(deletedMembershipWorkspace)

        val deletedWorkspace =
            Workspace.create("Theta", "desc", ownerAccountId).also {
                it.delete(UUID.fromString("00000000-0000-0000-0000-000000000911"))
            }
        workspaceJpaRepository.save(deletedWorkspace)

        val activeWorkspace = Workspace.create("Iota", "desc", ownerAccountId)
        workspaceJpaRepository.save(activeWorkspace)

        val memberships = workspaceMembershipQueryJpaRepository.findActiveWorkspaceMembershipsByAccountId(ownerAccountId)

        assertEquals(1, memberships.size)
        assertEquals("Iota", memberships.first().workspaceTitle)
        assertEquals(Role.OWNER, memberships.first().role)
    }
}

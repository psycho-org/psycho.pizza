package pizza.psycho.sos.workspace.infrastructure.persistence.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
class WorkspaceMembershipCommandJpaRepositoryTests {
    @Autowired
    private lateinit var workspaceMembershipCommandJpaRepository: WorkspaceMembershipCommandJpaRepository

    @Autowired
    private lateinit var workspaceCommandJpaRepository: WorkspaceCommandJpaRepository

    @Test
    fun `softDeleteActiveNonOwnerMembershipsByAccountId soft deletes only active non-owner memberships`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000921")

        val activeCrewWorkspace =
            Workspace.create("Crew Active", "desc", UUID.fromString("00000000-0000-0000-0000-000000000923")).also {
                it.addMembership(accountId, role = Role.CREW)
            }
        val deletedWorkspace =
            Workspace.create("Crew Deleted", "desc", UUID.fromString("00000000-0000-0000-0000-000000000924")).also {
                it.addMembership(accountId, role = Role.CREW)
                it.delete(UUID.fromString("00000000-0000-0000-0000-000000000925"))
            }
        val ownerWorkspace = Workspace.create("Owner", "desc", accountId)

        workspaceCommandJpaRepository.save(activeCrewWorkspace)
        workspaceCommandJpaRepository.save(deletedWorkspace)
        workspaceCommandJpaRepository.save(ownerWorkspace)

        val memberships = workspaceMembershipCommandJpaRepository.findActiveNonOwnerMembershipsByAccountId(accountId)

        assertEquals(1, memberships.size)
        assertEquals("Crew Active", memberships.first().workspace.name)
        assertEquals(Role.CREW, memberships.first().role)
        assertNull(memberships.first().deletedAt)
    }
}

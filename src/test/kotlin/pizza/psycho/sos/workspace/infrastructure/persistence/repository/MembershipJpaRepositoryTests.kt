package pizza.psycho.sos.workspace.infrastructure.persistence.repository

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
class MembershipJpaRepositoryTests {
    @Autowired
    private lateinit var membershipJpaRepository: MembershipJpaRepository

    @Autowired
    private lateinit var workspaceJpaRepository: WorkspaceJpaRepository

    @Test
    fun `existsActiveOwnerMembershipByAccountId returns true for active owner membership`() {
        val ownerAccountId = UUID.fromString("00000000-0000-0000-0000-000000000901")
        workspaceJpaRepository.save(Workspace.create("Alpha", "desc", ownerAccountId))

        val exists = membershipJpaRepository.existsActiveOwnerMembershipByAccountId(ownerAccountId)

        assertTrue(exists)
    }

    @Test
    fun `existsActiveOwnerMembershipByAccountId returns false for crew membership`() {
        val ownerAccountId = UUID.fromString("00000000-0000-0000-0000-000000000902")
        val crewAccountId = UUID.fromString("00000000-0000-0000-0000-000000000903")
        val workspace = Workspace.create("Beta", "desc", ownerAccountId)
        workspace.addMembership(crewAccountId, Role.CREW)
        workspaceJpaRepository.save(workspace)

        val exists = membershipJpaRepository.existsActiveOwnerMembershipByAccountId(crewAccountId)

        assertFalse(exists)
    }

    @Test
    fun `existsActiveOwnerMembershipByAccountId returns false for soft deleted membership`() {
        val ownerAccountId = UUID.fromString("00000000-0000-0000-0000-000000000904")
        val workspace = Workspace.create("Gamma", "desc", ownerAccountId)
        workspace.memberships.first().delete(UUID.fromString("00000000-0000-0000-0000-000000000905"))
        workspaceJpaRepository.save(workspace)

        val exists = membershipJpaRepository.existsActiveOwnerMembershipByAccountId(ownerAccountId)

        assertFalse(exists)
    }

    @Test
    fun `existsActiveOwnerMembershipByAccountId returns false for membership in deleted workspace`() {
        val ownerAccountId = UUID.fromString("00000000-0000-0000-0000-000000000906")
        val workspace =
            Workspace.create("Delta", "desc", ownerAccountId).also {
                it.delete(UUID.fromString("00000000-0000-0000-0000-000000000907"))
            }
        workspaceJpaRepository.save(workspace)

        val exists = membershipJpaRepository.existsActiveOwnerMembershipByAccountId(ownerAccountId)

        assertFalse(exists)
    }
}

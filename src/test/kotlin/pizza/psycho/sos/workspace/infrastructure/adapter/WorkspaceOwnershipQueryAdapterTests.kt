package pizza.psycho.sos.workspace.infrastructure.adapter

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import pizza.psycho.sos.workspace.domain.repository.WorkspaceMembershipQueryRepository
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkspaceOwnershipQueryAdapterTests {
    private val workspaceMembershipQueryRepository =
        org.mockito.Mockito.mock(WorkspaceMembershipQueryRepository::class.java)
    private val adapter = WorkspaceOwnershipQueryAdapter(workspaceMembershipQueryRepository)

    @Test
    fun `existsActiveOwnerMembershipByAccountId returns true when owner membership exists`() {
        val accountId = UUID.randomUUID()
        `when`(workspaceMembershipQueryRepository.existsActiveOwnerMembershipByAccountId(accountId)).thenReturn(true)

        val result = adapter.existsActiveOwnerMembershipByAccountId(accountId)

        assertTrue(result)
    }

    @Test
    fun `existsActiveOwnerMembershipByAccountId returns false when owner membership does not exist`() {
        val accountId = UUID.randomUUID()
        `when`(workspaceMembershipQueryRepository.existsActiveOwnerMembershipByAccountId(accountId)).thenReturn(false)

        val result = adapter.existsActiveOwnerMembershipByAccountId(accountId)

        assertFalse(result)
    }
}

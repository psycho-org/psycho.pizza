package pizza.psycho.sos.common.message.token.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pizza.psycho.sos.common.message.domain.MessageType
import pizza.psycho.sos.common.message.token.domain.model.entity.MailAuthToken
import pizza.psycho.sos.common.message.token.domain.repository.MailAuthTokenRepository
import java.util.UUID

@Repository
interface MailAuthTokenJpaRepository :
    MailAuthTokenRepository,
    JpaRepository<MailAuthToken, UUID> {
    override fun findByToken(token: String): MailAuthToken?

    @Query(
        """
        select t
        from MailAuthToken t
        left join fetch t.params
        where t.token = :token
        """,
    )
    override fun findByTokenWithParams(
        @Param("token") token: String,
    ): MailAuthToken?

    @Query(
        """
        select t
        from MailAuthToken t
        where t.mailType = :mailType
          and lower(t.targetEmail) = lower(:targetEmail)
          and (
                (:contextKey is null and t.contextKey is null)
                or t.contextKey = :contextKey
              )
          and t.verifiedAt is null
          and t.failureReason is null
        """,
    )
    override fun findPendingByTarget(
        @Param("mailType") mailType: MessageType,
        @Param("targetEmail") targetEmail: String,
        @Param("contextKey") contextKey: String?,
    ): List<MailAuthToken>
}

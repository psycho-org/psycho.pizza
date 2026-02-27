package pizza.psycho.sos.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.Hibernate
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity protected constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, unique = true, nullable = false)
    var id: UUID? = null,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: Instant? = null,
    @LastModifiedDate
    @Column(name = "updated_at", updatable = true)
    var updatedAt: Instant? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        val thisClass = Hibernate.getClass(this)
        val otherClass = Hibernate.getClass(other)

        if (thisClass != otherClass) return false

        other as BaseEntity

        if (id == null || other.id == null) return false

        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "${this::class.simpleName}(id=$id)"
}

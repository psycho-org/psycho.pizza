package pizza.psycho.sos.common.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.Instant
import java.util.UUID

@MappedSuperclass
abstract class BaseSoftDeletedEntity protected constructor(

    @Column(name = "deleted_at", updatable = true, nullable = true)
    var deletedAt: Instant? = null,

    @Column(name = "deleted_by", nullable = true)
    var deletedBy: UUID? = null

) : BaseEntity() {

    val isDeleted: Boolean
        get() = deletedAt != null

    fun delete(id: UUID) {
        this.deletedAt = Instant.now()
        this.deletedBy = id
    }

    fun restore() {
        this.deletedAt = null
    }

}

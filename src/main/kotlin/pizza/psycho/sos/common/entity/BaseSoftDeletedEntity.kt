package pizza.psycho.sos.common.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.Instant

@MappedSuperclass
abstract class BaseSoftDeletedEntity(

    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    var isDeleted: Boolean = false,

    @Column(name = "deleted_at", updatable = true, nullable = true)
    var deletedAt: Instant? = null,
) : BaseEntity() {

    fun restore() {
        isDeleted = false
        deletedAt = null
    }

}

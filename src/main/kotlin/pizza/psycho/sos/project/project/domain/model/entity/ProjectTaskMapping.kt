package pizza.psycho.sos.project.project.domain.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseEntity
import java.util.UUID

@Entity
@Table(name = "project_task_mapping")
class ProjectTaskMapping(
    @ManyToOne(fetch = FetchType.LAZY)
    var project: Project,
    @Column(name = "task_id", nullable = false)
    val taskId: UUID,
) : BaseEntity()

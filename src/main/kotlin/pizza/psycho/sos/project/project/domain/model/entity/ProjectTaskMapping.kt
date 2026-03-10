package pizza.psycho.sos.project.project.domain.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseEntity
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import java.util.UUID

@Entity
@Table(name = "project_task_mapping")
class ProjectTaskMapping(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    var project: Project,
    @Column(name = "task_id", nullable = false)
    val taskId: UUID,
    @Embedded
    val workspaceId: WorkspaceId,
) : BaseEntity()

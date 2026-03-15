package pizza.psycho.sos.project.sprint.domain.model.entity

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
@Table(name = "sprint_project_mapping")
class SprintProjectMapping(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    var sprint: Sprint,
    @Column(name = "project_id", nullable = false)
    var projectId: UUID,
    @Embedded
    val workspaceId: WorkspaceId,
) : BaseEntity()

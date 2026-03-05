package pizza.psycho.sos.analysis.application.service.dto

import java.util.UUID

sealed interface AnalysisCommand {
    data class Create(
        val workspaceId: UUID,
        val sprintId: UUID,
        val requesterId: UUID,
    ) : AnalysisCommand
}

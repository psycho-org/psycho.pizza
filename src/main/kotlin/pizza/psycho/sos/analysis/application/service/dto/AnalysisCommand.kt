package pizza.psycho.sos.analysis.application.service.dto

import java.util.UUID

/*
 * 서비스 입력 DTO
 */
sealed interface AnalysisCommand {
    data class Create(
        val workspaceId: UUID,
        val sprintId: UUID,
        val requesterId: UUID,
    ) : AnalysisCommand
}

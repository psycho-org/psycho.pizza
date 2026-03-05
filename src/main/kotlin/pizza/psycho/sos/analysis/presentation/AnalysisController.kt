package pizza.psycho.sos.analysis.presentation

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.analysis.application.service.AnalysisService
import pizza.psycho.sos.analysis.application.service.dto.AnalysisCommand
import pizza.psycho.sos.analysis.presentation.dto.AnalysisRequest
import pizza.psycho.sos.analysis.presentation.dto.AnalysisResponse
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/{workspaceId}/analysis")
class AnalysisController(
    private val analysisService: AnalysisService,
) {
    // TODO: swagger
    @PostMapping("/request")
    fun createAnalysisRequest(
        @PathVariable workspaceId: UUID,
        @Valid @RequestBody request: AnalysisRequest.Create,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<AnalysisResponse.Create> {
        val result =
            analysisService.createSprintAnalysisRequest(
                AnalysisCommand.Create(
                    workspaceId = workspaceId,
                    sprintId = request.target.sprintId,
                    requesterId = principal.accountId,
                ),
            )

        return responseOf(
            data =
                AnalysisResponse.Create(
                    analysisRequestId = result.id,
                    status = result.status,
                    createdAt = result.createdAt,
                ),
            status = HttpStatus.CREATED,
            message = "분석 요청이 성공적으로 생성되었습니다.",
        )
    }
}

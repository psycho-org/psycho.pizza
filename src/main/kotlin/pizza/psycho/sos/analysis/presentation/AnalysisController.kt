package pizza.psycho.sos.analysis.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.analysis.application.service.AnalysisService
import pizza.psycho.sos.analysis.application.service.dto.AnalysisCommand
import pizza.psycho.sos.analysis.presentation.dto.AnalysisRequest
import pizza.psycho.sos.analysis.presentation.dto.AnalysisResponse
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import java.util.UUID

@Tag(name = "Analysis API", description = "AI 분석 관련 API")
@RestController
@RequestMapping("/api/v1/{workspaceId}/analysis")
class AnalysisController(
    private val analysisService: AnalysisService,
) {
    @Operation(
        summary = "스프린트 분석 요청 생성",
        description = "특정 워크스페이스 내의 스프린트에 대한 AI 분석 요청을 생성합니다. 분석은 백그라운드에서 진행됩니다.",
    )
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAnalysisRequest(
        @Parameter(description = "워크스페이스 ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable workspaceId: UUID,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "분석 요청 페이로드", required = true)
        @Valid
        @RequestBody request: AnalysisRequest.CreateAnalysisRequest,
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<AnalysisResponse.CreateAnalysisRequestResponse> {
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
                AnalysisResponse.CreateAnalysisRequestResponse(
                    analysisRequestId = result.id,
                    status = result.status,
                    createdAt = result.createdAt,
                ),
            status = HttpStatus.CREATED,
            message = "분석 요청이 성공적으로 생성되었습니다.",
        )
    }
}

package pizza.psycho.sos.analysis.presentation

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.analysis.application.service.AnalysisRequestQueryService
import pizza.psycho.sos.analysis.application.service.AnalysisRequestService
import pizza.psycho.sos.analysis.application.service.dto.AnalysisCommand
import pizza.psycho.sos.analysis.presentation.dto.AnalysisRequest
import pizza.psycho.sos.analysis.presentation.dto.AnalysisResponse
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/analysis-requests")
class AnalysisController(
    private val analysisRequestService: AnalysisRequestService,
    private val analysisRequestQueryService: AnalysisRequestQueryService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAnalysisRequest(
        @Valid
        @RequestBody body: AnalysisRequest.CreateAnalysisRequest,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<AnalysisResponse.CreateAnalysisRequestResponse> {
        val result =
            analysisRequestService.createSprintAnalysisRequest(
                AnalysisCommand.Create(
                    workspaceId = body.workspaceId,
                    sprintId = body.sprintId,
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

    @GetMapping
    fun getAnalysisRequests(
        @RequestParam workspaceId: UUID,
        @RequestParam sprintId: UUID,
    ): ApiResponse<AnalysisResponse.GetAnalysisRequestList.Response> =
        responseOf(
            data =
                analysisRequestQueryService.getAnalysisRequests(
                    workspaceId = workspaceId,
                    sprintId = sprintId,
                ),
            status = HttpStatus.OK,
            message = "분석 요청 목록 조회에 성공했습니다.",
        )

    /*
    @GetMapping("/{analysisRequestId}/report")
    fun getAnalysisRequestReport(
        @PathVariable analysisRequestId: UUID,
    ): ApiResponse<AnalysisResponse.GetAnalysisRequestReport.Response> =
        responseOf(
            data = analysisRequestQueryService.getAnalysisRequestReport(
                analysisRequestId = analysisRequestId,
            ),
            status = HttpStatus.OK,
            message = "분석 요청 리포트 조회에 성공했습니다.",
        )
     */
}

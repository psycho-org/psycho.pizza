package pizza.psycho.sos.analysis.presentation

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.analysis.application.service.AnalysisLifecycleService
import pizza.psycho.sos.analysis.application.service.AnalysisRequestService
import pizza.psycho.sos.analysis.application.service.dto.AnalysisCommand
import pizza.psycho.sos.analysis.presentation.dto.AnalysisRequest
import pizza.psycho.sos.analysis.presentation.dto.AnalysisResponse
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal

@RestController
@RequestMapping("/api/v1/analysis-requests")
class AnalysisController(
    private val analysisRequestService: AnalysisRequestService,
    private val analysisLifecycleService: AnalysisLifecycleService,
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

    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.OK)
    fun createAnalysisReport(
        @RequestBody body: AnalysisRequest.CompleteAnalysisReport,
    ): ApiResponse<AnalysisResponse.CreateAnalysisReportResponse> {
        analysisRequestService.complete(id = body.jobId)

        return responseOf(
            status = HttpStatus.OK,
            message = "분석 요청이 완료(DONE) 처리되었습니다.",
        )
    }
}

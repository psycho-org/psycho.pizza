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
import pizza.psycho.sos.analysis.domain.vo.AnalysisRequestStatus
import pizza.psycho.sos.analysis.presentation.dto.AnalysisRequest
import pizza.psycho.sos.analysis.presentation.dto.AnalysisResponse
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/analysis")
class AnalysisController(
    private val analysisRequestService: AnalysisRequestService,
    private val analysisLifecycleService: AnalysisLifecycleService,
) {
    @PostMapping("/request")
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

    @PostMapping("/report")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAnalysisReport(
        @RequestBody body: AnalysisRequest.CreateAnalysisReport,
    ): ApiResponse<AnalysisResponse.CreateAnalysisReportResponse> {
        // TODO: secret 검증 로직 필요하면 추가

        /*
         * TODO: complete
         * - AnalysisRequest 상태 변경(RUNNING -> DONE)
         * - AnalysisReport 저장
         */

//        analysisLifecycleService.complete(
//            id = jobId,
//            result = result
//        )

        return responseOf(
            data =
                AnalysisResponse.CreateAnalysisReportResponse(
                    reportId = UUID.randomUUID(), // TODO: 실제 생성된 report id로 변경
                    status = AnalysisRequestStatus.DONE.name,
                ),
            status = HttpStatus.OK,
            message = "분석 결과가 성공적으로 저장되고 작업이 완료(DONE) 처리되었습니다.",
        )
    }
}

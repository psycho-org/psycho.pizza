package pizza.psycho.sos.analysis.presentation

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.analysis.application.service.AnalysisRequestQueryService
import pizza.psycho.sos.analysis.application.service.AnalysisRequestService
import pizza.psycho.sos.analysis.application.service.dto.AnalysisCommand
import pizza.psycho.sos.analysis.domain.exception.AnalysisErrorCode
import pizza.psycho.sos.analysis.presentation.dto.AnalysisRequest
import pizza.psycho.sos.analysis.presentation.dto.AnalysisResponse
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.workspace.application.port.WorkspaceMembershipExistencePort
import java.util.UUID

@RestController
@RequestMapping("/api/v1/workspace/{workspaceId}/analysis-requests")
class AnalysisController(
    private val analysisRequestService: AnalysisRequestService,
    private val analysisRequestQueryService: AnalysisRequestQueryService,
    private val workspaceMembershipExistenceService: WorkspaceMembershipExistencePort,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAnalysisRequest(
        @PathVariable workspaceId: UUID,
        @Valid
        @RequestBody body: AnalysisRequest.CreateAnalysisRequest,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<AnalysisResponse.CreateAnalysisRequestResponse> {
        validateWorkspaceMembership(workspaceId, principal.accountId)

        val result =
            analysisRequestService.createSprintAnalysisRequest(
                AnalysisCommand.Create(
                    workspaceId = workspaceId,
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
        @PathVariable workspaceId: UUID,
        @RequestParam sprintId: UUID,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<AnalysisResponse.GetAnalysisRequestList.Response> {
        validateWorkspaceMembership(workspaceId, principal.accountId)

        return responseOf(
            data =
                analysisRequestQueryService.getAnalysisRequests(
                    workspaceId = workspaceId,
                    sprintId = sprintId,
                ),
            status = HttpStatus.OK,
            message = "분석 요청 목록 조회에 성공했습니다.",
        )
    }

    @GetMapping("/{analysisRequestId}/report")
    fun getAnalysisRequestReport(
        @PathVariable workspaceId: UUID,
        @PathVariable analysisRequestId: UUID,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<AnalysisResponse.GetAnalysisRequestReport.Response> {
        validateWorkspaceMembership(workspaceId, principal.accountId)

        return responseOf(
            data =
                analysisRequestQueryService.getAnalysisRequestReport(
                    analysisRequestId = analysisRequestId,
                ),
            status = HttpStatus.OK,
            message = "분석 요청 리포트 조회에 성공했습니다.",
        )
    }

    private fun validateWorkspaceMembership(
        workspaceId: UUID,
        accountId: UUID,
    ) {
        val isValidMember =
            workspaceMembershipExistenceService.existsActiveMembership(
                workspaceId = workspaceId,
                accountId = accountId,
            )

        if (!isValidMember) {
            throw DomainException(AnalysisErrorCode.ANALYSIS_INVALID_MEMBERSHIP)
        }
    }
}

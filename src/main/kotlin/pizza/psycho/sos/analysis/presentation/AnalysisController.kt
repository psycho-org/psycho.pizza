package pizza.psycho.sos.analysis.presentation

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.analysis.application.service.AnalysisService
import pizza.psycho.sos.analysis.presentation.dto.AnalysisRequest
import pizza.psycho.sos.analysis.presentation.dto.AnalysisResponse
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf

@RestController
@RequestMapping("/api/v1/analysis")
class AnalysisController(
    private val analysisService: AnalysisService,
) {
    // TODO: swagger
    @PostMapping("/request")
    fun createAnalysisRequest(
        @Valid @RequestBody request: AnalysisRequest.Create,
    ): ApiResponse<AnalysisResponse.Create> {
        val analysisRequest =
            analysisService.createSprintAnalysisRequest(
                sprintId = request.target.sprintId,
            )

        return responseOf(
            data =
                AnalysisResponse.Create(
                    analysisRequestId = analysisRequest.id,
                    status = analysisRequest.status,
                    createdAt = analysisRequest.createdAt,
                ),
            status = HttpStatus.CREATED,
            message = "분석 요청이 성공적으로 생성되었습니다.",
        )
    }
}

package pizza.psycho.sos.analysis.presentation.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "분석 요청 DTO")
sealed interface AnalysisRequest {
    @Schema(description = "스프린트 분석 생성 요청 페이로드")
    data class CreateAnalysisRequest(
        @field:NotNull
        @param:Schema(description = "분석 대상 객체")
        var target: Target.Sprint,
    ) : AnalysisRequest

    @Schema(description = "분석 대상 타입")
    sealed interface Target {
        @Schema(description = "스프린트 대상 정보")
        data class Sprint(
            @field:NotNull
            @param:Schema(
                description = "분석할 스프린트의 고유 ID",
                example = "123e4567-e89b-12d3-a456-426614174000",
            )
            var sprintId: UUID,
        ) : Target
    }
}

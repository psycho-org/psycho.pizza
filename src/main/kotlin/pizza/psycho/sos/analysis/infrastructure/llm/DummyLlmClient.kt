package pizza.psycho.sos.analysis.infrastructure.llm

import org.springframework.stereotype.Component
import pizza.psycho.sos.analysis.application.port.LlmClient

// FIXME: 전체 동작을 확인하기 위한 코드입니다.
// 추후에 진짜 구현체는 OpenAiClient에 작성하고 이 클래스는 삭제 예정입니다.
@Component
class DummyLlmClient : LlmClient {
    override fun analyze(prompt: String): String =
        """
        {
          "summary": "현재 스프린트는 진행 중이며 일부 지연 위험이 있습니다.",
          "risks": ["대기 중인 작업 증가", "특정 담당자 편중"],
          "topIssues": ["핵심 태스크 지연 가능성"],
          "recommendations": ["우선순위 재조정", "병목 작업 점검"]
        }
        """.trimIndent()
}

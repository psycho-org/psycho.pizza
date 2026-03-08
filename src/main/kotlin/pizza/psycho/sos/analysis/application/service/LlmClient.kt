package pizza.psycho.sos.analysis.application.service

/*
 * LlmClient
 * - 외부 API 호출
 */
interface LlmClient {
    fun analyze(prompt: String): String
}

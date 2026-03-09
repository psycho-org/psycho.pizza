package pizza.psycho.sos.analysis.application.port

/*
 * LlmClient
 * - 외부 API 호출
 */
interface LlmClient {
    fun analyze(prompt: String): String
}

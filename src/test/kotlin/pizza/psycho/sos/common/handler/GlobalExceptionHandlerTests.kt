package pizza.psycho.sos.common.handler

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.identity.account.domain.exception.AccountErrorCode
import pizza.psycho.sos.identity.challenge.domain.exception.ChallengeErrorCode
import java.time.Instant

class GlobalExceptionHandlerTests {
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(TestController())
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

    @Test
    fun `domain exception with error code uses mapped status and code`() {
        mockMvc
            .perform(get("/test/error-coded").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Account not found"))
    }

    @Test
    fun `domain exception with cooldown meta adds retry after header and body meta`() {
        mockMvc
            .perform(get("/test/cooldown").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isTooManyRequests)
            .andExpect(header().string(HttpHeaders.RETRY_AFTER, "43"))
            .andExpect(jsonPath("$.code").value("CHALLENGE_OTP_COOLDOWN_ACTIVE"))
            .andExpect(jsonPath("$.meta.availableAt").exists())
            .andExpect(jsonPath("$.meta.retryAfterSeconds").value(43))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `legacy domain exception falls back to bad request without code`() {
        mockMvc
            .perform(get("/test/legacy").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").doesNotExist())
            .andExpect(jsonPath("$.message").value("legacy bad request"))
    }

    @RestController
    @RequestMapping("/test")
    class TestController {
        @GetMapping("/error-coded")
        fun errorCoded(): String = throw DomainException(AccountErrorCode.ACCOUNT_NOT_FOUND)

        @GetMapping("/cooldown")
        fun cooldown(): String =
            throw DomainException(
                errorCode = ChallengeErrorCode.CHALLENGE_OTP_COOLDOWN_ACTIVE,
                meta =
                    ErrorMeta.Cooldown(
                        availableAt = Instant.parse("2026-03-15T00:00:43Z"),
                        retryAfterSeconds = 43,
                    ),
            )

        @GetMapping("/legacy")
        @Suppress("DEPRECATION")
        fun legacy(): String = throw DomainException("legacy bad request")
    }
}

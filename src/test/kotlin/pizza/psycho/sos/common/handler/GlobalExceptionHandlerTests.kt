package pizza.psycho.sos.common.handler

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.identity.account.domain.exception.AccountErrorCode

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

        @GetMapping("/legacy")
        @Suppress("DEPRECATION")
        fun legacy(): String = throw DomainException("legacy bad request")
    }
}

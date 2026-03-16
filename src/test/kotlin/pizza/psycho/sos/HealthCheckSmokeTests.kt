package pizza.psycho.sos

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthCheckSmokeTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun getActuatorHealthReturns200() {
        mockMvc
            .perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }
}

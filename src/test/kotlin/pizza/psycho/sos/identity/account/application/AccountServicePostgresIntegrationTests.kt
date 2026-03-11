package pizza.psycho.sos.identity.account.application

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import pizza.psycho.sos.common.domain.vo.Email
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.application.service.dto.RegisterAccountResult
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import pizza.psycho.sos.identity.challenge.domain.Challenge
import pizza.psycho.sos.identity.challenge.domain.ConfirmationToken
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import pizza.psycho.sos.identity.challenge.infrastructure.ChallengeRepository
import pizza.psycho.sos.identity.challenge.infrastructure.ConfirmationTokenRepository
import pizza.psycho.sos.identity.challenge.support.PostgresTestContainerSupport
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("tc")
@Testcontainers
@SpringBootTest
@ActiveProfiles("test-tc")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountServicePostgresIntegrationTests : PostgresTestContainerSupport() {
    @Autowired
    private lateinit var accountService: AccountService

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var challengeRepository: ChallengeRepository

    @Autowired
    private lateinit var confirmationTokenRepository: ConfirmationTokenRepository

    @BeforeEach
    fun cleanUp() {
        confirmationTokenRepository.deleteAllInBatch()
        challengeRepository.deleteAllInBatch()
        accountRepository.deleteAllInBatch()
    }

    @Test
    fun `concurrent register requests with the same confirmation token allow only one success`() {
        val token = confirmationTokenRepository.saveAndFlush(newConfirmationToken(email = "user@psycho.pizza"))
        val command =
            AccountCommand.Register(
                confirmationTokenId = token.id(),
                password = "Password123!@#",
                firstName = "Rick",
                lastName = "Sanchez",
            )

        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)

        try {
            val futures =
                List(2) {
                    executor.submit<RegisterAccountResult> {
                        ready.countDown()
                        start.await(5, TimeUnit.SECONDS)
                        accountService.register(command)
                    }
                }

            assertTrue(ready.await(5, TimeUnit.SECONDS))

            val results =
                assertTimeoutPreemptively(Duration.ofSeconds(10)) {
                    start.countDown()
                    futures.map { it.get(10, TimeUnit.SECONDS) }
                }

            assertEquals(1, results.count { it is RegisterAccountResult.Success })
            assertEquals(1, results.count { it is RegisterAccountResult.Failure.InvalidConfirmationToken })
            assertEquals(1L, accountRepository.count())
            assertTrue(confirmationTokenRepository.findById(token.id()).orElseThrow().used)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun newConfirmationToken(email: String): ConfirmationToken {
        val challenge =
            challengeRepository.saveAndFlush(
                Challenge.create(
                    operationType = OperationType.REGISTER,
                    targetEmail = Email.of(email),
                    otpHash = "otp-hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 3,
                ),
            )

        return ConfirmationToken
            .create(
                challenge = challenge,
                operationType = OperationType.REGISTER,
                targetEmail = Email.of(email),
                expiresAt = Instant.now().plusSeconds(300),
            )
    }
}

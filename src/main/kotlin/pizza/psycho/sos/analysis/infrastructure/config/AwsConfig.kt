package pizza.psycho.sos.analysis.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@Configuration
class AwsConfig {
    @Value("\${aws.region:ap-northeast-2}")
    private lateinit var region: String

    @Bean
    fun sqsAsyncClient(): SqsAsyncClient =
        SqsAsyncClient
            .builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()
}

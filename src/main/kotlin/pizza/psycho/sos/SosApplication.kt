package pizza.psycho.sos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SosApplication

fun main(args: Array<String>) {
    runApplication<SosApplication>(*args)
}

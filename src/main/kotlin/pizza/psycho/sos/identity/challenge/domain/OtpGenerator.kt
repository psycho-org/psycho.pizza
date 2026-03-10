package pizza.psycho.sos.identity.challenge.domain

interface OtpGenerator {
    fun generate(length: Int): String
}

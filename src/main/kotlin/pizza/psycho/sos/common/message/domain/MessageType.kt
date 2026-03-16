package pizza.psycho.sos.common.message.domain

enum class MessageType(
    val supportedChannels: Set<MessageChannel>,
) {
    OTP(setOf(MessageChannel.EMAIL)),
    WORKSPACE_INVITE(setOf(MessageChannel.EMAIL)),
    EMAIL_ALREADY_EXISTS(setOf(MessageChannel.EMAIL)),
}

package pizza.psycho.sos.common.support.transaction.helper

import org.hibernate.exception.ConstraintViolationException

fun Throwable.hasConstraintName(constraintName: String): Boolean =
    generateSequence(this) { it.cause }
        .filterIsInstance<ConstraintViolationException>()
        .firstOrNull()
        ?.constraintName
        ?.equals(constraintName, ignoreCase = true)
        ?: false

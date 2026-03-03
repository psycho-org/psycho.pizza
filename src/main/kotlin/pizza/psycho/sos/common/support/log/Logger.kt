package pizza.psycho.sos.common.support.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty

/**
 * ### 사용법
 *
 * class 내에서 사용
 * ```
 * class Test {
 *     private val log by loggerDelegate()
 *
 *     fun testMethod() {
 *         log.info("test method")
 *     }
 * }
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> T.loggerDelegate(): LoggerDelegate<T> {
    val actualClass = (if (this::class.isCompanion) {
        this::class.java.enclosingClass ?: this::class.java
    } else {
        this::class.java
    }) as Class<T>

    return LoggerDelegate(actualClass)
}

class LoggerDelegate<in T : Any>(private val clazz: Class<T>) {
    private val logger by lazy { LoggerFactory.getLogger(clazz) }

    operator fun getValue(thisRef: T, property: KProperty<*>): Logger = logger
}

/**
 * ### 사용법
 *
 * 클래스 밖에서 선언하여 사용
 *
 * ```kotlin
 * private val log = logger()
 *
 * fun Test.toTest(): TestResult.Success = ...
 *
 * class Test{
 *     ...
 * }
 * ```
 */
fun logger(): Logger = LoggerFactory.getLogger(object {}.javaClass.enclosingClass)

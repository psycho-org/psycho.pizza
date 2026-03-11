package pizza.psycho.sos.common.patch

sealed interface Patch<out T> {
    // 변경 안 함
    data object Undefined : Patch<Nothing>

    // nullable 필드 clear
    data object Clear : Patch<Nothing>

    // 실제 값으로 변경
    data class Value<T>(
        val value: T,
    ) : Patch<T>
}

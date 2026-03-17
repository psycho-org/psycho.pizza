package pizza.psycho.sos.common.event

/**
 * 아래 코드를 엔티티에 정의
 *
 * ```
 * @Transient
 * private var events: MutableSet<DomainEvent>? = null
 *
 * private fun ensureEvents(): MutableSet<DomainEvent> {
 *     if (events == null) {
 *         events = mutableSetOf()
 *     }
 *     return events!!
 * }
 *
 * override fun registerEvent(event: DomainEvent) {
 *     ensureEvents() += event
 * }
 *
 * override fun domainEvents(): List<DomainEvent> =
 *     events?.toList() ?: emptyList()
 *
 * override fun pullDomainEvents(): List<DomainEvent> =
 *     domainEvents().also { events?.clear() }
 * ```
 */
interface AggregateRoot {
    /**
     * 도메인 이벤트 등록
     *
     * @param event 등록할 도메인 이벤트
     */
    fun registerEvent(event: DomainEvent)

    /**
     * 현재 AggregateRoot가 가지고 있는 모든 도메인 이벤트 조회
     *
     * @return 등록된 이벤트들의 리스트 (복사본)
     */
    fun domainEvents(): List<DomainEvent>

    /**
     * 현재 AggregateRoot가 가지고 있는 모든 도메인 이벤트 조회 및 초기화
     */
    fun pullDomainEvents(): List<DomainEvent>
}

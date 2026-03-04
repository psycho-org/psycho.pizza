package pizza.psycho.sos.common.event

/**
 * 아래와 같이 위임받아 사용
 *
 * ```
 * class Order :
 *     BaseEntity(...),
 *     AggregateRoot by DomainEventDelegate()
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

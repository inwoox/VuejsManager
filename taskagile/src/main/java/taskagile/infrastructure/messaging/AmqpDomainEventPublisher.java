package taskagile.infrastructure.messaging;

import taskagile.domain.common.event.DomainEvent;
import taskagile.domain.common.event.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AmqpDomainEventPublisher implements DomainEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(AmqpDomainEventPublisher.class);

  private RabbitTemplate rabbitTemplate;
  private FanoutExchange exchange;

  public AmqpDomainEventPublisher(RabbitTemplate rabbitTemplate,
                                  @Qualifier("domainEventsExchange") FanoutExchange exchange) {
    this.rabbitTemplate = rabbitTemplate;
    this.exchange = exchange;
  }

  // 도메인 이벤트를 RabbitMQ 메시지로 만들어서 전송하기 위해 rabbitTemplate의 convertAndSend() 메서드를 호출
  @Override
  public void publish(DomainEvent event) {
    log.debug("Publishing domain event: " + event);
    try {
      rabbitTemplate.convertAndSend(exchange.getName(), "", event);
    } catch (AmqpException e) {
      log.error("Failed to send domain event to MQ", e);
    }
  }
}

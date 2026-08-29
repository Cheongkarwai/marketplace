package com.cheong.userservice.listener;

import com.cheong.common.core.reactive.event.CustomerCreatedEvent;
import com.cheong.userservice.filter.EmailDuplicateFilter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Getter
@Slf4j
@Component
@KafkaListener(
        topics = "${app.kafka.topics.customer-events:customer.events}",
        groupId = "${spring.kafka.consumer.group-id:user-service-customer-group}"
)
public class CustomerTopicListener {

    private final BlockingQueue<CustomerCreatedEvent> receivedEvents = new LinkedBlockingQueue<>();
    private final EmailDuplicateFilter emailDuplicateFilter;

    public CustomerTopicListener(EmailDuplicateFilter emailDuplicateFilter) {
        this.emailDuplicateFilter = emailDuplicateFilter;
    }

    @KafkaHandler
    public Mono<Void> handleCustomerCreated(
            @Payload CustomerCreatedEvent event,
            @Header(value = "eventType", required = false) String eventType,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.info("Received CustomerCreatedEvent payload from Kafka: key={}, eventType={}, eventId={}, customerId={}, email={}",
                key, eventType, event.eventId(), event.customerId(), event.emailAddress());

        receivedEvents.offer(event);
        return processCustomerCreated(event);
    }

    public Mono<Void> processCustomerCreated(CustomerCreatedEvent event) {
        log.info("Processing business logic for created customer: {}", event.customerId());
        return emailDuplicateFilter.register(event.emailAddress())
                .then();
    }


}

package com.cheong.common.r2dbc.outbox;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

@Service
public class OutboxService {

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public OutboxService(R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
    }

    public Mono<Outbox> saveEvent(String aggregateType, String aggregateId, String eventType, String jsonPayload) {
        Outbox outbox = Outbox.builder()
                .id(UUID.randomUUID().toString())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .type(eventType)
                .payload(jsonPayload)
                .timestamp(Instant.now())
                .isNew(true)
                .build();

        return r2dbcEntityTemplate.insert(outbox);
    }

    public <T> Mono<Outbox> saveEvent(String aggregateType, String aggregateId, String eventType, T payload, Function<T, String> serializer) {
        return Mono.fromCallable(() -> serializer.apply(payload))
                .flatMap(jsonPayload -> saveEvent(aggregateType, aggregateId, eventType, jsonPayload));
    }

    public Mono<Outbox> saveEvent(OutboxEvent outboxEvent, Function<Object, String> serializer) {
        return Mono.fromCallable(() -> serializer.apply(outboxEvent.getPayload()))
                .flatMap(jsonPayload -> saveEvent(
                        outboxEvent.getAggregateType(),
                        outboxEvent.getAggregateId(),
                        outboxEvent.getEventType(),
                        jsonPayload
                ));
    }
}

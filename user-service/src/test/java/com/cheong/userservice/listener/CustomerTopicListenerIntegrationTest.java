package com.cheong.userservice.listener;

import com.cheong.userservice.event.CustomerCreatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest(properties = {
        "spring.kafka.consumer.group-id=listener-test-${random.uuid}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
class CustomerTopicListenerIntegrationTest {

    @Container
    @ServiceConnection
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.1");

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private CustomerTopicListener customerTopicListener;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should consume and deserialize CustomerCreatedEvent from Testcontainers Kafka")
    void shouldConsumeAndDeserializeCustomerCreatedEvent() throws Exception {
        String customerId = UUID.randomUUID().toString();
        CustomerCreatedEvent event = new CustomerCreatedEvent(
                UUID.randomUUID().toString(),
                customerId,
                "Testcontainers",
                "Tester",
                LocalDate.of(1992, 8, 15),
                "testcontainers_" + customerId.substring(0, 8) + "@example.com",
                "60112233445",
                Instant.now()
        );

        String payloadJson = objectMapper.writeValueAsString(event);

        ProducerRecord<String, String> record = new ProducerRecord<>("customer.events", customerId, payloadJson);
        record.headers().add("eventType", "CUSTOMER_CREATED".getBytes(StandardCharsets.UTF_8));
        record.headers().add("__TypeId__", "CUSTOMER_CREATED".getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);

        CustomerCreatedEvent receivedEvent = null;
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            CustomerCreatedEvent evt = customerTopicListener.getReceivedEvents().poll(2, TimeUnit.SECONDS);
            if (evt != null && customerId.equals(evt.customerId())) {
                receivedEvent = evt;
                break;
            }
        }

        assertNotNull(receivedEvent, "CustomerTopicListener should receive CustomerCreatedEvent payload from Testcontainers Kafka");
        assertEquals(customerId, receivedEvent.customerId());
        assertEquals("Testcontainers", receivedEvent.firstName());
        assertEquals("Tester", receivedEvent.lastName());
        assertEquals(event.emailAddress(), receivedEvent.emailAddress());
        assertEquals(event.mobileNumber(), receivedEvent.mobileNumber());
    }
}

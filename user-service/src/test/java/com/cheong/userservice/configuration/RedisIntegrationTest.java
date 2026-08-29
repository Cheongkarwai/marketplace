package com.cheong.userservice.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest
class RedisIntegrationTest {

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private ReactiveRedisConnectionFactory connectionFactory;

    @Autowired
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Autowired
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Autowired(required = false)
    private org.redisson.api.RedissonClient redissonClient;

    @Test
    @DisplayName("Verify ReactiveRedisConnectionFactory is active and responds to PING")
    void testRedisConnectionFactoryPing() {
        assertNotNull(connectionFactory, "ReactiveRedisConnectionFactory should not be null");

        Mono<String> pingResult = connectionFactory.getReactiveConnection().ping();

        StepVerifier.create(pingResult)
                .expectNext("PONG")
                .verifyComplete();
    }

    @Test
    @DisplayName("Verify ReactiveStringRedisTemplate set and get operations")
    void testStringRedisTemplateOperations() {
        String key = "test:string:" + UUID.randomUUID();
        String value = "hello-reactive-redis";

        Mono<String> flow = reactiveStringRedisTemplate.opsForValue()
                .set(key, value, Duration.ofSeconds(60))
                .then(reactiveStringRedisTemplate.opsForValue().get(key))
                .flatMap(retrieved -> reactiveStringRedisTemplate.delete(key).thenReturn(retrieved));

        StepVerifier.create(flow)
                .expectNext(value)
                .verifyComplete();
    }

    @Test
    @DisplayName("Verify ReactiveRedisTemplate object serialization and deserialization with Jackson")
    void testReactiveRedisTemplateObjectSerialization() {
        String key = "test:object:" + UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "id", UUID.randomUUID().toString(),
                "name", "RedisTestUser",
                "active", true
        );

        Mono<Object> flow = reactiveRedisTemplate.opsForValue()
                .set(key, payload, Duration.ofSeconds(60))
                .then(reactiveRedisTemplate.opsForValue().get(key))
                .flatMap(retrieved -> reactiveRedisTemplate.delete(key).thenReturn(retrieved));

        StepVerifier.create(flow)
                .assertNext(retrieved -> {
                    assertNotNull(retrieved, "Retrieved object should not be null");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> retrievedMap = (Map<String, Object>) retrieved;
                    assertEquals(payload.get("id"), retrievedMap.get("id"));
                    assertEquals(payload.get("name"), retrievedMap.get("name"));
                    assertEquals(payload.get("active"), retrievedMap.get("active"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Verify RedissonClient is available and can execute operations")
    void testRedissonClientOperations() {
        assertNotNull(redissonClient, "RedissonClient bean should be created");
        var bucket = redissonClient.getBucket("test:redisson:" + UUID.randomUUID());
        bucket.set("redisson-val", Duration.ofSeconds(60));
        assertEquals("redisson-val", bucket.get());
        bucket.delete();
    }
}

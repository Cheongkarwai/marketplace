package com.cheong.userservice.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class DuplicateFilterIntegrationTest {

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private EmailDuplicateFilter emailDuplicateFilter;

    @Autowired
    private DuplicateFilterManager duplicateFilterManager;

    @Test
    @DisplayName("Verify EmailDuplicateFilter bean can register and detect duplicates directly")
    void testEmailDuplicateFilterDirectly() {
        String testEmail = "test-" + UUID.randomUUID() + "@example.com";

        // 1. Not duplicate initially
        StepVerifier.create(emailDuplicateFilter.isDuplicate(testEmail))
                .expectNext(false)
                .verifyComplete();

        // 2. Register the email
        StepVerifier.create(emailDuplicateFilter.register(testEmail))
                .expectNext(true)
                .verifyComplete();

        // 3. Now it should be detected as duplicate
        StepVerifier.create(emailDuplicateFilter.isDuplicate(testEmail))
                .expectNext(true)
                .verifyComplete();

        // 4. Registering again should return false (already present)
        StepVerifier.create(emailDuplicateFilter.register(testEmail))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("Verify DuplicateFilterManager coordinates and delegates to EmailDuplicateFilter")
    void testDuplicateFilterManagerDelegation() {
        String email = "manager-" + UUID.randomUUID() + "@example.com";

        // Check duplicate via manager
        StepVerifier.create(duplicateFilterManager.checkDuplicate(DuplicateFilterName.EMAIL, email))
                .expectNext(false)
                .verifyComplete();

        // Track value via manager
        StepVerifier.create(duplicateFilterManager.trackValue(DuplicateFilterName.EMAIL, email))
                .expectNext(true)
                .verifyComplete();

        // Check duplicate via manager again
        StepVerifier.create(duplicateFilterManager.checkDuplicate(DuplicateFilterName.EMAIL, email))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Verify batch registration with registerAll via manager")
    void testBatchRegistration() {
        String email1 = "batch-1-" + UUID.randomUUID() + "@example.com";
        String email2 = "batch-2-" + UUID.randomUUID() + "@example.com";

        Mono<Long> batchFlow = duplicateFilterManager.trackValue(DuplicateFilterName.EMAIL, List.of(email1, email2));

        StepVerifier.create(batchFlow)
                .assertNext(count -> assertTrue(count >= 1, "Should add at least one element"))
                .verifyComplete();

        StepVerifier.create(emailDuplicateFilter.isDuplicate(email1))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(emailDuplicateFilter.isDuplicate(email2))
                .expectNext(true)
                .verifyComplete();
    }
}

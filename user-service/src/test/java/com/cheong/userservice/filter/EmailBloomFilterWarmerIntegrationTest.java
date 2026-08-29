package com.cheong.userservice.filter;

import com.cheong.userservice.model.Contact;
import com.cheong.userservice.model.Customer;
import com.cheong.userservice.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class EmailBloomFilterWarmerIntegrationTest {

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmailDuplicateFilter emailDuplicateFilter;

    @Autowired
    private EmailBloomFilterWarmer emailBloomFilterWarmer;

    @Test
    @DisplayName("Verify warm-up and rebuild loads existing customer emails from database into Bloom filter")
    void testWarmUpAndRebuildFromDatabase() {
        String suffix1 = UUID.randomUUID().toString().substring(0, 8);
        String suffix2 = UUID.randomUUID().toString().substring(0, 8);
        String email1 = "warmer_" + suffix1 + "@example.com";
        String email2 = "warmer_" + suffix2 + "@example.com";

        String fax1 = "021" + suffix1.replaceAll("[^0-9]", "1").substring(0, 7);
        String fax2 = "022" + suffix2.replaceAll("[^0-9]", "2").substring(0, 7);

        Customer customer1 = new Customer("Warmer1", "Test", LocalDate.of(1990, 1, 1),
                new Contact(email1, "011" + suffix1.replaceAll("[^0-9]", "1").substring(0, 7), fax1));
        Customer customer2 = new Customer("Warmer2", "Test", LocalDate.of(1992, 2, 2),
                new Contact(email2, "012" + suffix2.replaceAll("[^0-9]", "2").substring(0, 7), fax2));

        // 1. Save customers to PostgreSQL
        customerRepository.save(customer1).block();
        customerRepository.save(customer2).block();

        // 2. Rebuild Bloom filter from DB
        StepVerifier.create(emailBloomFilterWarmer.rebuildFilter())
                .assertNext(count -> assertTrue(count >= 2, "Should have loaded at least the 2 saved customer emails"))
                .verifyComplete();

        // 3. Verify both emails are now recognized as duplicates by Bloom filter
        StepVerifier.create(emailDuplicateFilter.isDuplicate(email1))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(emailDuplicateFilter.isDuplicate(email2))
                .expectNext(true)
                .verifyComplete();

        // 4. Verify unknown email is not in Bloom filter
        StepVerifier.create(emailDuplicateFilter.isDuplicate("unknown_" + UUID.randomUUID() + "@example.com"))
                .expectNext(false)
                .verifyComplete();

        // 5. Subsequent warm-up detects already populated filter
        StepVerifier.create(emailBloomFilterWarmer.warmUpFilter())
                .assertNext(count -> assertTrue(count >= 2, "Should return existing count without re-inserting"))
                .verifyComplete();
    }
}

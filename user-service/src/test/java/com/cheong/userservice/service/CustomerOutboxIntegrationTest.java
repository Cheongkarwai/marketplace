package com.cheong.userservice.service;

import com.cheong.common.r2dbc.outbox.Outbox;
import com.cheong.userservice.dto.ContactDTO;
import com.cheong.userservice.dto.CustomerCreationDTO;
import com.cheong.userservice.dto.CustomerDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CustomerOutboxIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Test
    void createCustomer_shouldPersistCustomerAndOutboxRecordInDatabase() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "cdc_test_" + uniqueSuffix + "@example.com";
        String mobile = "601" + uniqueSuffix.replaceAll("[^0-9]", "9").substring(0, 7);
        String fax = "602" + uniqueSuffix.replaceAll("[^0-9]", "8").substring(0, 7);
        CustomerCreationDTO creationDTO = new CustomerCreationDTO(
                "Debezium",
                "Tester",
                LocalDate.of(1995, 5, 20),
                new ContactDTO(email, mobile, fax)
        );

        CustomerDTO createdCustomer = customerService.createCustomer(creationDTO).block();

        assertNotNull(createdCustomer);
        assertNotNull(createdCustomer.id());
        assertEquals("Debezium", createdCustomer.firstName());
        assertEquals("Tester", createdCustomer.lastName());

        // Verify outbox entry in database
        StepVerifier.create(
                r2dbcEntityTemplate.select(Outbox.class)
                        .matching(Query.query(Criteria.where("aggregateid").is(createdCustomer.id())))
                        .one()
        )
        .assertNext(outbox -> {
            assertNotNull(outbox.getId());
            assertEquals("customer", outbox.getAggregateType());
            assertEquals(createdCustomer.id(), outbox.getAggregateId());
            assertEquals("CUSTOMER_CREATED", outbox.getType());
            assertNotNull(outbox.getPayload());
            assertTrue(outbox.getPayload().contains(email), "Payload should contain customer email");
            assertTrue(outbox.getPayload().contains(createdCustomer.id()), "Payload should contain customer id");
            assertNotNull(outbox.getTimestamp());
        })
        .verifyComplete();
    }
}

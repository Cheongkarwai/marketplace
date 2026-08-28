package com.cheong.userservice.service;

import com.cheong.common.r2dbc.outbox.Outbox;
import com.cheong.common.r2dbc.outbox.OutboxService;
import com.cheong.userservice.dto.ContactDTO;
import com.cheong.userservice.dto.CustomerCreationDTO;
import com.cheong.userservice.dto.CustomerDTO;
import com.cheong.userservice.mapper.CustomerMapper;
import com.cheong.userservice.model.Contact;
import com.cheong.userservice.model.Customer;
import com.cheong.userservice.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private OutboxService outboxService;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, customerMapper, outboxService, objectMapper);
    }

    @Test
    void createCustomer_shouldSaveCustomerAndOutboxEvent() {
        String customerId = UUID.randomUUID().toString();
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        ContactDTO contactDTO = new ContactDTO("john@example.com", "1234567890", null);
        CustomerCreationDTO creationDTO = new CustomerCreationDTO("John", "Doe", birthDate, contactDTO);

        Customer customer = new Customer("John", "Doe", birthDate, new Contact("john@example.com", "1234567890", null));
        Customer savedCustomer = new Customer("John", "Doe", birthDate, new Contact("john@example.com", "1234567890", null));
        savedCustomer.setId(customerId);

        CustomerDTO expectedDTO = new CustomerDTO(customerId, "johndoe", "John", "Doe", contactDTO, birthDate);

        Outbox outbox = Outbox.builder()
                .id(UUID.randomUUID().toString())
                .aggregateType("customer")
                .aggregateId(customerId)
                .type("CUSTOMER_CREATED")
                .payload("{\"customerId\":\"" + customerId + "\"}")
                .timestamp(Instant.now())
                .build();

        when(customerMapper.mapToCustomer(creationDTO)).thenReturn(customer);
        when(customerRepository.save(customer)).thenReturn(Mono.just(savedCustomer));
        when(outboxService.saveEvent(eq("customer"), eq(customerId), eq("CUSTOMER_CREATED"), any(), any()))
                .thenReturn(Mono.just(outbox));
        when(customerMapper.mapToCustomerDTO(savedCustomer)).thenReturn(expectedDTO);

        StepVerifier.create(customerService.createCustomer(creationDTO))
                .assertNext(dto -> {
                    assertEquals(customerId, dto.id());
                    assertEquals("John", dto.firstName());
                    assertEquals("Doe", dto.lastName());
                })
                .verifyComplete();

        verify(customerRepository, times(1)).save(customer);
        verify(outboxService, times(1)).saveEvent(eq("customer"), eq(customerId), eq("CUSTOMER_CREATED"), any(), any());
    }
}

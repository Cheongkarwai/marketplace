package com.cheong.userservice.service;

import com.cheong.common.core.reactive.event.CustomerCreatedEvent;
import com.cheong.common.r2dbc.outbox.OutboxService;
import com.cheong.userservice.dto.CustomerCreationDTO;
import com.cheong.userservice.dto.CustomerDTO;
import com.cheong.userservice.filter.EmailDuplicateFilter;
import com.cheong.userservice.mapper.CustomerMapper;
import com.cheong.userservice.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final EmailDuplicateFilter emailDuplicateFilter;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerMapper customerMapper,
                           OutboxService outboxService,
                           ObjectMapper objectMapper,
                           EmailDuplicateFilter emailDuplicateFilter) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
        this.emailDuplicateFilter = emailDuplicateFilter;
    }

    @Transactional
    public Mono<CustomerDTO> createCustomer(CustomerCreationDTO customerCreationDTO) {
        return Mono.justOrEmpty(customerCreationDTO)
                .map(customerMapper::mapToCustomer)
                .filterWhen(customer -> emailDuplicateFilter.isDuplicate(customer.getContact().getEmailAddress())
                        .map(isDuplicate-> !isDuplicate))
                .flatMap(customerRepository::save)
                .flatMap(savedCustomer -> {
                    CustomerCreatedEvent event = new CustomerCreatedEvent(
                            UUID.randomUUID().toString(),
                            savedCustomer.getId(),
                            savedCustomer.getFirstName(),
                            savedCustomer.getLastName(),
                            savedCustomer.getBirthDate(),
                            savedCustomer.getContact() != null ? savedCustomer.getContact().getEmailAddress() : null,
                            savedCustomer.getContact() != null ? savedCustomer.getContact().getMobileNumber() : null,
                            Instant.now()
                    );

                    return outboxService.saveEvent("customer", savedCustomer.getId(), "CUSTOMER_CREATED", event, this::serializeToJson)
                            .doOnNext(outbox -> log.info("Outbox event saved with id: {} for customer: {}", outbox.getId(), savedCustomer.getId()))
                            .thenReturn(savedCustomer);
                })
                .map(customerMapper::mapToCustomerDTO);
    }

    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}

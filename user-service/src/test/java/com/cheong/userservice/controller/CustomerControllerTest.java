package com.cheong.userservice.controller;

import com.cheong.userservice.dto.ContactDTO;
import com.cheong.userservice.dto.CustomerCreationDTO;
import com.cheong.userservice.dto.CustomerDTO;
import com.cheong.userservice.service.CustomerService;
import com.cheong.userservice.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private SearchService searchService;

    @Test
    void createCustomer_shouldReturn201Created() {
        String customerId = UUID.randomUUID().toString();
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        ContactDTO contactDTO = new ContactDTO("john@example.com", "1234567890", "1234567890");
        CustomerCreationDTO creationDTO = new CustomerCreationDTO("John", "Doe", birthDate, contactDTO);
        CustomerDTO customerDTO = new CustomerDTO(customerId, "johndoe", "John", "Doe", contactDTO, birthDate);

        when(customerService.createCustomer(any(CustomerCreationDTO.class))).thenReturn(Mono.just(customerDTO));

        webTestClient.post()
                .uri("/api/customers")
                .header("X-API-VERSION", "v1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(creationDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/customers/" + customerId);
    }
}

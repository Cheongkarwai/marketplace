package com.cheong.userservice.controller;

import com.cheong.userservice.dto.CustomerCreationDTO;
import com.cheong.userservice.dto.CustomerDTO;
import com.cheong.userservice.service.CustomerService;
import com.cheong.userservice.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer APIs", description = "Create, read, update, and delete customer profile")
public class CustomerController {

    private final SearchService searchService;
    private final CustomerService customerService;

    public CustomerController(SearchService searchService,
                              CustomerService customerService) {
        this.searchService = searchService;
        this.customerService = customerService;
    }

    @GetMapping(":search")
    public Flux<CustomerDTO> search(@RequestParam LocalDate birthDate) {
        return searchService.searchCustomers(birthDate);
    }

    @PostMapping
    @Operation(summary = "Creates a customer profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer profile is created", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Internal server error", value = """
                             {
                                 "title": "Internal server error",
                                 "status": 500,
                                 "detail": "Internal server error",
                                 "instance": "/api/customers",
                             }
                            """)

            ))
    })
    public Mono<ResponseEntity<Void>> createCustomer(@Validated @RequestBody CustomerCreationDTO customerCreationDTO,
                                                     ServerHttpRequest serverHttpRequest) {

        return customerService.createCustomer(customerCreationDTO)
                .map(customerDTO -> {
                    URI location = UriComponentsBuilder.fromPath(serverHttpRequest.getPath().value())
                            .path("/{id}")
                            .buildAndExpand(customerDTO.id())
                            .toUri();

                    return ResponseEntity.created(location).build();
                });

    }
}

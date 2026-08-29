package com.cheong.userservice.repository;

import com.cheong.userservice.model.Customer;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface CustomerRepository extends R2dbcRepository<Customer, String> {

    Flux<Customer> findByBirthDate(LocalDate birthDate);

    Mono<Boolean> existsByContact_EmailAddress(String emailAddress);

    @Query("SELECT email_address FROM customer WHERE email_address IS NOT NULL")
    Flux<String> findAllEmailAddresses();
}

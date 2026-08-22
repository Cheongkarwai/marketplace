package com.cheong.userservice.repository;

import com.cheong.userservice.model.Customer;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface CustomerRepository extends R2dbcRepository<Customer, String> {

    Flux<Customer> findByBirthDate(LocalDate birthDate);

}

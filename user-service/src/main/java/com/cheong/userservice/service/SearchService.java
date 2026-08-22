package com.cheong.userservice.service;

import com.cheong.userservice.dto.CustomerDTO;
import com.cheong.userservice.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
public class SearchService {

    private final CustomerRepository customerRepository;

    public SearchService(CustomerRepository customerRepository){
        this.customerRepository = customerRepository;
    }

    public Flux<CustomerDTO> searchCustomers(LocalDate birthDate) {
        //return customerRepository.findByBirthDate(birthDate);
        return Flux.empty();
    }
}

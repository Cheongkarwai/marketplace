package com.cheong.userservice.service;

import com.cheong.userservice.dto.CustomerCreationDTO;
import com.cheong.userservice.dto.CustomerDTO;
import com.cheong.userservice.mapper.CustomerMapper;
import com.cheong.userservice.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerMapper customerMapper){
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    public Mono<CustomerDTO> createCustomer(CustomerCreationDTO customerCreationDTO){
        return Mono.justOrEmpty(customerCreationDTO)
                .map(customerMapper::mapToCustomer)
                .doOnNext(customer -> {
                    log.info("Customer email address {}", customer.getContact().getEmailAddress());
                })
                .flatMap(customerRepository::save)
                .map(customerMapper::mapToCustomerDTO);
    }
}

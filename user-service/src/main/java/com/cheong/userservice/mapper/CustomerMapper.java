package com.cheong.userservice.mapper;

import com.cheong.userservice.dto.CustomerCreationDTO;
import com.cheong.userservice.dto.CustomerDTO;
import com.cheong.userservice.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {

    Customer mapToCustomer(CustomerCreationDTO customerCreationDTO);

    CustomerDTO mapToCustomerDTO(Customer customer);
}

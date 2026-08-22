package com.cheong.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

    @Column("email_address")
    private String emailAddress;

    @Column("mobile_number")
    private String mobileNumber;

    @Column("fax_number")
    private String faxNumber;
}

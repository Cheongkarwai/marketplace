package com.cheong.userservice.model;

import com.cheong.common.r2dbc.model.BaseEntity;
import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Table("customer")
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity<String> {

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("birth_date")
    private LocalDate birthDate;

    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    private Contact contact;

}

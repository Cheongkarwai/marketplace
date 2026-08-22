package com.cheong.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CustomerCreationDTO(
        @NotBlank(message = "{firstName.notBlank}")
        @Size(max = 100, message = "{firstName.size}")
        String firstName,
        @NotBlank(message = "{lastName.notBlank}")
        @Size(max = 100, message = "{lastName.size}")
        String lastName,
        @NotNull(message = "{birthDate.notBlank}")
        @Past(message = "{birthDate.past}")
        LocalDate birthDate,
        @NotNull(message = "{contact.notBlank}")
        ContactDTO contact
) {
}

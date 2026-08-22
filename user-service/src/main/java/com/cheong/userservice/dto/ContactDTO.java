package com.cheong.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactDTO(
        @NotBlank(message = "{email.notBlank}")
        @Email(message = "{email.invalid}")
        @Size(max = 100, message = "{email.size}")
        String emailAddress,
        @NotBlank(message = "{mobileNumber.notBlank}")
        @Size(max = 100, message = "{mobileNumber.size}")
        String mobileNumber,
        @NotBlank(message = "{faxNumber.notBlank}")
        @Size(max = 100, message = "{faxNumber.size}")
        String faxNumber) {
}

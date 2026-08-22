package com.cheong.userservice.dto;

import java.time.LocalDate;

public record CustomerDTO(
        String id,
        String username,
        String firstName,
        String lastName,
        ContactDTO contactDTO,
        LocalDate birthDate) {
}

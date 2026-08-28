package com.cheong.userservice.event;

import java.time.Instant;
import java.time.LocalDate;

public record CustomerCreatedEvent(
        String eventId,
        String customerId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String emailAddress,
        String mobileNumber,
        Instant occurredAt
) {
}

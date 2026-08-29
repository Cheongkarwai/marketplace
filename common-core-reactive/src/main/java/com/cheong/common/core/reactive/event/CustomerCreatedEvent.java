package com.cheong.common.core.reactive.event;

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
        Instant eventTimestamp
) implements Event{
    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public Instant getEventTimestamp() {
        return eventTimestamp;
    }
}

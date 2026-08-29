package com.cheong.common.core.reactive.event;

import java.time.Instant;

public interface Event {

    String getEventId();
    Instant getEventTimestamp();
}

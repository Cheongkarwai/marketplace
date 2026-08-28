package com.cheong.common.r2dbc.outbox;

public interface OutboxEvent {
    String getAggregateType();
    String getAggregateId();
    String getEventType();
    Object getPayload();
}

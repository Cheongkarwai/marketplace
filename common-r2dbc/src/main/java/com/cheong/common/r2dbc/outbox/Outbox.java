package com.cheong.common.r2dbc.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("outbox")
public class Outbox implements Persistable<String> {

    @Id
    @Column("id")
    private String id;

    @Column("aggregatetype")
    private String aggregateType;

    @Column("aggregateid")
    private String aggregateId;

    @Column("type")
    private String type;

    @Column("payload")
    private String payload;

    @Column("timestamp")
    private Instant timestamp;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}

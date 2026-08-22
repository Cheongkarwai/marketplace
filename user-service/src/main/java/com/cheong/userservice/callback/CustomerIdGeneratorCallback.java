package com.cheong.userservice.callback;

import com.cheong.userservice.model.Customer;
import org.jspecify.annotations.NullMarked;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CustomerIdGeneratorCallback implements BeforeConvertCallback<Customer> {

    @Override
    @NullMarked
    public Publisher<Customer> onBeforeConvert(Customer entity, SqlIdentifier table) {
        if (!StringUtils.hasText(entity.getId())) {
            entity.setId(UUID.randomUUID().toString());
        }
        return Mono.just(entity);
    }
}
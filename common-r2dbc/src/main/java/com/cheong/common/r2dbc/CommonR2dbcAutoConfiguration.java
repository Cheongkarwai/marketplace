package com.cheong.common.r2dbc;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

import com.cheong.common.r2dbc.outbox.OutboxService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;

@AutoConfiguration(afterName = {
        "org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration",
        "org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcAutoConfiguration"
})
@ConditionalOnClass({ConnectionFactory.class, R2dbcEntityTemplate.class})
@EnableR2dbcAuditing
public class CommonR2dbcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxService outboxService(R2dbcEntityTemplate r2dbcEntityTemplate) {
        return new OutboxService(r2dbcEntityTemplate);
    }
}

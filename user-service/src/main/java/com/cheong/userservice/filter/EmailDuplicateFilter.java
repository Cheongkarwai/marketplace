package com.cheong.userservice.filter;

import com.cheong.userservice.filter.bloom.AbstractRedissonBloomFilter;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dedicated duplicate filter for customer email addresses using a Redisson Bloom filter.
 */
@Component
public class EmailDuplicateFilter extends AbstractRedissonBloomFilter<String> {

    public EmailDuplicateFilter(
            RedissonReactiveClient redissonReactiveClient,
            @Value("${filter.email.key:filter:email}") String filterKey,
            @Value("${filter.email.error-rate:0.001}") double errorRate,
            @Value("${filter.email.capacity:100000}") long capacity) {
        super(redissonReactiveClient, filterKey, errorRate, capacity);
    }
}

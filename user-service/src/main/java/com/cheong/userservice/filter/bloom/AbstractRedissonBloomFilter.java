package com.cheong.userservice.filter.bloom;

import com.cheong.userservice.filter.GenericDuplicateFilter;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilterReactive;
import org.redisson.api.RedissonReactiveClient;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Reusable abstract base class for Bloom filter duplicate detection using Redisson.
 * Uses standard Redis commands (bitsets) so it is compatible with all Redis deployments
 * (including vanilla Redis, Redis Stack, AWS ElastiCache, GCP Memorystore).
 *
 * @param <T> The element type stored in the Bloom filter.
 */
@Getter
@Slf4j
public abstract class AbstractRedissonBloomFilter<T> implements GenericDuplicateFilter<T> {

    private final RBloomFilterReactive<T> bloomFilter;
    private final String filterKey;
    private final double errorRate;
    private final long capacity;

    protected AbstractRedissonBloomFilter(RedissonReactiveClient redissonReactiveClient,
                                          String filterKey,
                                          double errorRate,
                                          long capacity) {
        this.filterKey = filterKey;
        this.bloomFilter = redissonReactiveClient.getBloomFilter(filterKey);
        this.errorRate = errorRate;
        this.capacity = capacity;
    }

    @PostConstruct
    public void init() {
        initialize().block();
    }

    @Override
    public Mono<Void> initialize() {
        return bloomFilter.tryInit(capacity, errorRate).then();
    }

    @Override
    public Mono<Boolean> isDuplicate(T value) {
        return bloomFilter.contains(value);
    }

    @Override
    public Mono<Boolean> register(T value) {
        return bloomFilter.add(value);
    }

    @Override
    public Mono<Long> registerAll(Collection<T> values) {
        return bloomFilter.add(values);
    }

}

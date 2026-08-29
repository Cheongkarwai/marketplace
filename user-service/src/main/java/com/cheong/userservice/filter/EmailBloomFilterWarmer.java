package com.cheong.userservice.filter;

import com.cheong.userservice.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * Warms up the Redis Bloom filter with existing customer emails from PostgreSQL.
 * Ensures the Bloom filter is pre-populated upon initial deployment or cache invalidation.
 */
@Slf4j
@Component
public class EmailBloomFilterWarmer implements ApplicationRunner {

    private static final String WARMUP_LOCK = "lock:filter:email:warmup";
    private static final int BATCH_SIZE = 1000;

    private final CustomerRepository customerRepository;
    private final EmailDuplicateFilter emailDuplicateFilter;
    private final RedissonReactiveClient redissonClient;

    public EmailBloomFilterWarmer(CustomerRepository customerRepository,
                                  EmailDuplicateFilter emailDuplicateFilter,
                                  RedissonReactiveClient redissonClient) {
        this.customerRepository = customerRepository;
        this.emailDuplicateFilter = emailDuplicateFilter;
        this.redissonClient = redissonClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        warmUpFilter().subscribe(
                count -> log.info("Email Bloom filter initialization completed. Total emails loaded: {}", count),
                error -> log.error("Failed to initialize Email Bloom filter from database", error)
        );
    }

    /**
     * Warms up the Bloom filter if it is not yet initialized or populated in Redis.
     * Uses a Redisson distributed lock to prevent concurrent initialization across multiple instances.
     *
     * @return Mono emitting the number of emails loaded into the filter.
     */
    public Mono<Long> warmUpFilter() {
        RLockReactive lock = redissonClient.getLock(WARMUP_LOCK);
        long threadId = Thread.currentThread().threadId();

        return lock.tryLock(5, 60, TimeUnit.SECONDS, threadId)
                .flatMap(acquired -> {
                    if (!acquired) {
                        log.info("Another instance is currently warming up the Email Bloom filter. Skipping.");
                        return Mono.just(0L);
                    }

                    return doWarmUp()
                            .flatMap(count -> lock.unlock(threadId).thenReturn(count))
                            .onErrorResume(error -> lock.unlock(threadId).then(Mono.error(error)));
                });
    }

    /**
     * Forces a full reset and re-population of the Bloom filter from PostgreSQL.
     *
     * @return Mono emitting the total number of re-indexed emails.
     */
    public Mono<Long> rebuildFilter() {
        RLockReactive lock = redissonClient.getLock(WARMUP_LOCK);
        long threadId = Thread.currentThread().threadId();

        return lock.tryLock(5, 120, TimeUnit.SECONDS, threadId)
                .flatMap(acquired -> {
                    if (!acquired) {
                        return Mono.error(new IllegalStateException("Cannot rebuild filter; another instance holds the lock."));
                    }

                    log.warn("Rebuilding Email Bloom filter from PostgreSQL database...");
                    return emailDuplicateFilter.getBloomFilter().delete()
                            .then(emailDuplicateFilter.initialize())
                            .then(loadAndPopulate())
                            .flatMap(count -> lock.unlock(threadId).thenReturn(count))
                            .onErrorResume(error -> lock.unlock(threadId).then(Mono.error(error)));
                });
    }

    private Mono<Long> doWarmUp() {
        return emailDuplicateFilter.getBloomFilter().isExists()
                .flatMap(exists -> {
                    if (!exists) {
                        log.info("Email Bloom filter not found in Redis. Initializing and loading from database...");
                        return emailDuplicateFilter.initialize().then(loadAndPopulate());
                    }

                    return emailDuplicateFilter.getBloomFilter().count()
                            .flatMap(count -> {
                                if (count == 0L) {
                                    log.info("Email Bloom filter is empty. Loading existing emails from database...");
                                    return loadAndPopulate();
                                }
                                log.info("Email Bloom filter already warmed with {} elements. Skipping DB warm-up.", count);
                                return Mono.just(count);
                            });
                });
    }

    private Mono<Long> loadAndPopulate() {
        return customerRepository.findAllEmailAddresses()
                .buffer(BATCH_SIZE)
                .flatMap(emailDuplicateFilter::registerAll)
                .reduce(0L, Long::sum)
                .doOnNext(total -> log.info("Loaded and registered {} emails into Bloom filter", total));
    }
}

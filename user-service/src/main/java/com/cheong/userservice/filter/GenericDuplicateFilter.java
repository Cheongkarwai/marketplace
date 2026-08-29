package com.cheong.userservice.filter;

import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Generic contract for checking and registering duplicate values reactively.
 *
 * @param <T> The type of value being filtered for duplicates.
 */
public interface GenericDuplicateFilter<T> {

    /**
     * Initializes the underlying filter storage (e.g. Bloom filter capacity and error rate).
     */
    Mono<Void> initialize();

    /**
     * Checks whether the specified value already exists in the filter.
     *
     * @param value The value to check.
     * @return Mono emitting true if the value is likely a duplicate, false otherwise.
     */
    Mono<Boolean> isDuplicate(T value);

    /**
     * Registers a single value in the filter.
     *
     * @param value The value to register.
     * @return Mono emitting true if the element was added, false if already contained.
     */
    Mono<Boolean> register(T value);

    /**
     * Registers multiple values in the filter.
     *
     * @param values The collection of values to register.
     * @return Mono emitting the number of newly added elements.
     */
    Mono<Long> registerAll(Collection<T> values);
}

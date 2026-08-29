package com.cheong.userservice.filter;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

/**
 * Service registry and coordinator for duplicate filters by name.
 * Delegates execution to specialized {@link GenericDuplicateFilter} beans.
 */
@Service
public class DuplicateFilterManager {

    private final Map<DuplicateFilterName, GenericDuplicateFilter<String>> registries;

    public DuplicateFilterManager(EmailDuplicateFilter emailDuplicateFilter) {
        this.registries = Map.of(
                DuplicateFilterName.EMAIL, emailDuplicateFilter
        );
    }

    public Mono<Boolean> checkDuplicate(DuplicateFilterName duplicateFilterName, String value) {
        GenericDuplicateFilter<String> filter = registries.get(duplicateFilterName);
        if (filter == null) {
            return Mono.error(new IllegalArgumentException("Duplicate filter not found for: " + duplicateFilterName));
        }
        return filter.isDuplicate(value);
    }

    public Mono<Boolean> trackValue(DuplicateFilterName duplicateFilterName, String value) {
        GenericDuplicateFilter<String> filter = registries.get(duplicateFilterName);
        if (filter == null) {
            return Mono.error(new IllegalArgumentException("Duplicate filter not found for: " + duplicateFilterName));
        }
        return filter.register(value);
    }

    public Mono<Long> trackValue(DuplicateFilterName duplicateFilterName, Collection<String> values) {
        GenericDuplicateFilter<String> filter = registries.get(duplicateFilterName);
        if (filter == null) {
            return Mono.error(new IllegalArgumentException("Duplicate filter not found for: " + duplicateFilterName));
        }
        return filter.registerAll(values);
    }
}

package com.nitin.foodorder.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Builds a safe Pageable: caps the page size and only sorts by whitelisted fields. */
final class PageUtil {

    private static final int MAX_SIZE = 50;

    private PageUtil() {
    }

    static Pageable of(int page, int size, String sortBy, String direction, Set<String> allowedSorts,
                       String defaultSort) {
        String field = allowedSorts.contains(sortBy) ? sortBy : defaultSort;
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE), Sort.by(dir, field));
    }
}

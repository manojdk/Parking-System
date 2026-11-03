package com.parkingSystem.controller.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.parkingSystem.exception.BadRequestException;

/**
 * Builds a validated {@link Pageable} from the {@code page} and {@code size} request parameters.
 * <p>
 * The sort order is fixed by each endpoint and always ends with the primary key, so the order is total and
 * consecutive pages never repeat or skip a row.
 */
public final class Pagination {

    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final int MAX_SIZE = 100;

    private Pagination() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        if (page < 0) {
            throw new BadRequestException("page must be 0 or more");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_SIZE);
        }
        return PageRequest.of(page, size, sort);
    }
}

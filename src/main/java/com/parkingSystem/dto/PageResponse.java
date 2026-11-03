package com.parkingSystem.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * One page of a list endpoint. Every paginated endpoint returns this shape.
 *
 * @param content       the items on this page
 * @param page          zero-based page number
 * @param size          requested page size
 * @param totalElements items across all pages
 * @param totalPages    number of pages
 * @param last          true if this is the last page
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<? super E, ? extends T> mapper) {
        List<T> content = page.getContent().stream().<T>map(mapper).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }
}

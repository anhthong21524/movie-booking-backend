package com.moviebooking.common.util;

import io.quarkus.panache.common.Sort;

public class SortUtil {

    private SortUtil() {}

    public static Sort parse(String sortParam, String defaultField) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(defaultField, Sort.Direction.Descending);
        }
        String[] parts = sortParam.split(":");
        if (parts.length != 2) {
            return Sort.by(defaultField, Sort.Direction.Descending);
        }
        Sort.Direction dir = "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.Ascending
                : Sort.Direction.Descending;
        return Sort.by(parts[0], dir);
    }
}

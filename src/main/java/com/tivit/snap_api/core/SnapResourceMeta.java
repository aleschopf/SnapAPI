package com.tivit.snap_api.core;

import com.tivit.snap_api.enums.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public record SnapResourceMeta(
        String path,
        List<Endpoint> endpoints,
        List<String> searchableFields,
        JpaRepository<?, ?> repository,
        Class<?> entityClass,
        Class<?> idClass,
        boolean supportsSpecification
) {
    public boolean isEndpointEnabled(Endpoint endpoint) {
        return endpoints.contains(endpoint);
    }
}
package com.tivit.snap_api.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tivit.snap_api.core.SnapRegistry;
import com.tivit.snap_api.core.SnapResourceMeta;
import com.tivit.snap_api.dto.PageResponse;
import com.tivit.snap_api.enums.Endpoint;
import com.tivit.snap_api.spec.SnapSpecBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@SuppressWarnings("unchecked")
@RequestMapping("${snap.api.base-path:/api}")
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class SnapGenericController {
    private static final Logger log = LoggerFactory.getLogger(SnapGenericController.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public SnapGenericController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{resource}")
    public ResponseEntity<?> findAll(
            @PathVariable String resource,
            @RequestParam Map<String, String> queryParams,
            Pageable pageable) {

        SnapResourceMeta meta = SnapRegistry.getMetaFor(resource);
        if (meta == null || !meta.isEndpointEnabled(Endpoint.GET_ALL)) {
            return ResponseEntity.notFound().build();
        }

        try {
            JpaRepository<Object, Object> repo = (JpaRepository<Object, Object>) meta.repository();

            if (meta.supportsSpecification() && !meta.searchableFields().isEmpty()) {
                JpaSpecificationExecutor<Object> specRepo = (JpaSpecificationExecutor<Object>) repo;
                Specification<Object> spec = SnapSpecBuilder.build(meta, queryParams);
                Page<Object> page = specRepo.findAll(spec, pageable);
                return ResponseEntity.ok(PageResponse.from(page));
            } else {
                Page<Object> page = repo.findAll(pageable);
                return ResponseEntity.ok(PageResponse.from(page));
            }
        } catch (Exception e) {
            log.error("Error executing findAll for resource {}: {}", resource, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to execute query: " + e.getMessage()));
        }
    }

    @GetMapping("/{resource}/{id}")
    public ResponseEntity<?> findById(@PathVariable String resource, @PathVariable String id) {
        SnapResourceMeta meta = SnapRegistry.getMetaFor(resource);
        if (meta == null || !meta.isEndpointEnabled(Endpoint.GET_BY_ID)) {
            return ResponseEntity.notFound().build();
        }

        try {
            JpaRepository<Object, Object> repo = (JpaRepository<Object, Object>) meta.repository();
            Object idValue = convertId(id, meta.idClass());
            Optional<Object> entity = repo.findById(idValue);
            return entity.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error finding resource {} with id {}: {}", resource, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to find entity: " + e.getMessage()));
        }
    }

    @PostMapping("/{resource}")
    public ResponseEntity<?> create(@PathVariable String resource, @RequestBody Map<String, Object> body) {
        SnapResourceMeta meta = SnapRegistry.getMetaFor(resource);
        if (meta == null || !meta.isEndpointEnabled(Endpoint.CREATE)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Object entity = objectMapper.convertValue(body, meta.entityClass());
            JpaRepository<Object, Object> repo = (JpaRepository<Object, Object>) meta.repository();
            Object saved = repo.save(entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error creating resource {}: {}", resource, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create entity: " + e.getMessage()));
        }
    }

    @PutMapping("/{resource}/{id}")
    public ResponseEntity<?> update(
            @PathVariable String resource,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {

        SnapResourceMeta meta = SnapRegistry.getMetaFor(resource);
        if (meta == null || !meta.isEndpointEnabled(Endpoint.EDIT)) {
            return ResponseEntity.notFound().build();
        }

        try {
            JpaRepository<Object, Object> repo = (JpaRepository<Object, Object>) meta.repository();
            Object idValue = convertId(id, meta.idClass());

            if (!repo.existsById(idValue)) {
                return ResponseEntity.notFound().build();
            }

            body.put("id", idValue);

            Object entity = objectMapper.convertValue(body, meta.entityClass());
            Object updated = repo.save(entity);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating resource {} with id {}: {}", resource, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update entity: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{resource}/{id}")
    public ResponseEntity<?> delete(@PathVariable String resource, @PathVariable String id) {
        SnapResourceMeta meta = SnapRegistry.getMetaFor(resource);
        if (meta == null || !meta.isEndpointEnabled(Endpoint.DELETE)) {
            return ResponseEntity.notFound().build();
        }

        try {
            JpaRepository<Object, Object> repo = (JpaRepository<Object, Object>) meta.repository();
            Object idValue = convertId(id, meta.idClass());

            if (!repo.existsById(idValue)) {
                return ResponseEntity.notFound().build();
            }

            repo.deleteById(idValue);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting resource {} with id {}: {}", resource, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete entity: " + e.getMessage()));
        }
    }

    private Object convertId(String id, Class<?> idClass) {
        if (idClass == Long.class || idClass == long.class) {
            return Long.valueOf(id);
        } else if (idClass == Integer.class || idClass == int.class) {
            return Integer.valueOf(id);
        } else if (idClass == String.class) {
            return id;
        } else if (idClass == UUID.class) {
            return UUID.fromString(id);
        }

        return id;
    }
}
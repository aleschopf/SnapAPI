package com.tivit.snap_api.init;

import com.tivit.snap_api.annotations.SnapResource;
import com.tivit.snap_api.core.SnapRegistry;
import com.tivit.snap_api.core.SnapResourceMeta;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Component
public class SnapInitializer {
    private static final Logger log = LoggerFactory.getLogger(SnapInitializer.class);

    private final ApplicationContext context;
    private final Repositories repositories;

    public SnapInitializer(ApplicationContext context) {
        this.context = context;
        this.repositories = new Repositories(context);
    }

    @PostConstruct
    public void init() {
        log.info("Initializing SnapREST resources");

        Map<String, Object> beans = context.getBeansWithAnnotation(SnapResource.class);

        for (Object bean : beans.values()) {
            Class<?> entityClass = bean.getClass();
            processEntity(entityClass);
        }

        for (Class<?> entityType : repositories) {
            processEntity(entityType);
        }

        log.info("SnapREST initialized with {} resources", SnapRegistry.getAll().size());
    }

    private void processEntity(Class<?> entityClass) {
        SnapResource annotation = AnnotationUtils.findAnnotation(entityClass, SnapResource.class);
        if (annotation == null) return;

        if (annotation.searchableFields().length > 0) {
            validateSearchableFields(entityClass, annotation.searchableFields());
        }

        Optional<Object> repoObj = repositories.getRepositoryFor(entityClass);
        
        if (repoObj.isEmpty()) {
            log.warn("Entity {} has @SnapResource but no repository found", entityClass.getName());
            return;
        }

        if (!(repoObj.get() instanceof JpaRepository<?, ?> repository)) {
            log.warn("Repository for {} is not a JpaRepository", entityClass.getName());
            return;
        }

        Class<?> idClass = getIdClass(repository);
        boolean supportsSpec = repository instanceof JpaSpecificationExecutor;

        if (!supportsSpec && annotation.searchableFields().length > 0) {
            log.warn("Entity {} has searchableFields but repository doesn't implement JpaSpecificationExecutor",
                    entityClass.getName());
        }

        SnapResourceMeta meta = new SnapResourceMeta(
                annotation.path(),
                Arrays.asList(annotation.expose()),
                Arrays.asList(annotation.searchableFields()),
                repository,
                entityClass,
                idClass,
                supportsSpec
        );

        SnapRegistry.register(annotation.path(), meta);
        log.info("Registered SnapResource: {}", annotation.path());
    }

    private Class<?> getIdClass(JpaRepository<?, ?> repository) {
        try {
            for (Type type : repository.getClass().getGenericInterfaces()) {
                if (type instanceof ParameterizedType paramType) {
                    if (paramType.getRawType().equals(JpaRepository.class)) {
                        return (Class<?>) paramType.getActualTypeArguments()[1];
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not determine ID class for repository {}", repository.getClass().getName(), e);
        }
        return Object.class;
    }

    private void validateSearchableFields(Class<?> entityClass, String[] searchableFields) {
        for (String fieldPath : searchableFields) {
            String[] parts = fieldPath.split("\\.");
            Class<?> currentClass = entityClass;

            for (String part : parts) {
                try {
                    Field field = currentClass.getDeclaredField(part);
                    currentClass = field.getType();
                } catch (NoSuchFieldException e) {
                    throw new IllegalStateException(
                            "Campo '" + fieldPath + "' não encontrado na entidade " +
                                    entityClass.getSimpleName());
                }
            }
        }
    }
}
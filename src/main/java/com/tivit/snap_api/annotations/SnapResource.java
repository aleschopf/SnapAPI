package com.tivit.snap_api.annotations;

import com.tivit.snap_api.enums.Endpoint;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SnapResource {
    String path();
    Endpoint[] expose() default {};
    String[] searchableFields() default {};
}
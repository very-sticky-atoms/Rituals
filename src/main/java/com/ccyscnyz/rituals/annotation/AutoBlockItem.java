package com.ccyscnyz.rituals.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Map;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoBlockItem {
    String tab() default "";
}
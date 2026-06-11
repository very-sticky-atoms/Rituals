package com.ccyscnyz.rituals.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoBlockItem {
    String tab() default "";
    int maxStackSize() default 64;
    int durability() default 0;
}